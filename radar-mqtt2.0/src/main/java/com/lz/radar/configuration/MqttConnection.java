package com.lz.radar.configuration;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lz.radar.domain.IotDevice;
import com.lz.radar.domain.IotTelemetry;
import com.lz.radar.service.IotDeviceService;
import com.lz.radar.service.IotTelemetryService;
import com.lz.radar.utils.SnowflakeIdGenerator;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 封装单个 MQTT 客户端连接与监听逻辑
 */
public class MqttConnection {

    /** MQTT 客户端实例 */
    private MqttClient client;

    /** MQTT 配置 JSON（来自数据库） */
    private final String configJson;

    /** 协议名称（用于日志输出） */
    private final String protocolName;

    /** 对应设备ID（来自协议-设备关联表） */
    private final Long deviceId;

    private final Long protocolId;

    /** 是否正在运行检测线程 */
    private volatile boolean running = false;

    /** 入库队列：按设备ID隔离，避免两台设备消息互相串 */
    private final Queue<Map<String, Object>> messageQueue = new ConcurrentLinkedQueue<>();

    /** 所有设备共享一个合并数据Map，key固定为1L */
    private static final long COMBINED_KEY = 1L;
    private static final Map<Long, Map<String, Object>> latestDataMap = new ConcurrentHashMap<>();

    /** MyBatis Mapper（写入数据库） */

    private final IotTelemetryService iotTelemetryService;

    private final IotDeviceService iotDeviceService;

    public MqttConnection(String protocolName, String configJson, Long deviceId, Long protocolId,
                          IotTelemetryService iotTelemetryService, IotDeviceService iotDeviceService) {
        this.protocolName = protocolName;
        this.configJson = configJson;
        this.deviceId = deviceId;
        this.protocolId = protocolId;
        this.iotTelemetryService = iotTelemetryService;
        this.iotDeviceService = iotDeviceService;
    }

    /**
     * 启动 MQTT 连接与订阅 逻辑
     */
    public synchronized void connect() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode node = mapper.readTree(configJson);

            String broker = node.get("host").asText();   // MQTT服务器地址
            String topic = node.get("topic").asText();   // 订阅主题
            String username = node.has("username") ? node.get("username").asText() : null;
            String password = node.has("password") ? node.get("password").asText() : null;

            System.out.println("🔗 [" + protocolName + "] 正在连接 Broker: " + broker);
            System.out.println("📡 [" + protocolName + "] 订阅主题: " + topic);

            String clientId = "mqtt_" + protocolName + "_" + System.currentTimeMillis();
            client = new MqttClient(broker, clientId, new MemoryPersistence());

            // 设置连接选项
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(true);
            options.setAutomaticReconnect(true);
            options.setConnectionTimeout(10);
            if (username != null) options.setUserName(username);
            if (password != null) options.setPassword(password.toCharArray());

            // 设置回调函数
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectionLost(Throwable cause) {
                    System.err.println("❌ [" + protocolName + "] 连接丢失: " + cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    String payload = new String(message.getPayload());
                    System.out.println("📩 收到消息: topic=" + topic + " payload=" + payload);

                    try {
                        ObjectMapper mapper = new ObjectMapper();
                        Map<String, Object> dataMap = mapper.readValue(payload, Map.class);

                        // ✅ 判断是否为 OneNET 物模型格式，如果是则转换
                        if (dataMap.containsKey("params")) {
                            dataMap = convertOneNETFormat(dataMap);
                            System.out.println("🔄 已转换 OneNET 物模型格式: " + dataMap);
                        }

                        // ✅ 1. 入队列（用于入库）
                        messageQueue.add(dataMap);

                        // ✅ 2. 更新最新值（所有设备数据合并到同一个key，支持两个雷达拼凑完整参数）
                        Map<String, Object> existing = latestDataMap.getOrDefault(COMBINED_KEY, new HashMap<>());
                        existing.putAll(dataMap);
                        existing.put("lastUpdate", System.currentTimeMillis());
                        latestDataMap.put(COMBINED_KEY, existing);

                    } catch (Exception e) {
                        System.err.println("🚫 消息解析失败: " + e.getMessage());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    System.out.println("✅ [" + protocolName + "] 消息发送完成");
                }

                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    System.out.println("🔗 [" + protocolName + "] 已连接到: " + serverURI + (reconnect ? "（重连）" : ""));
                    try {
                        client.subscribe(topic, 1);
                        System.out.println("📡 [" + protocolName + "] 已订阅主题: " + topic);

                        // ✅ 更新设备状态为在线
                        if (iotDeviceService != null){
                            IotDevice device = new IotDevice();
                            device.setDeviceId(deviceId);
                            device.setStatus("online");
                            iotDeviceService.updateDeviceStatus(device);
                            System.out.println("🟢 [" + protocolName + "] 设备已标记为在线");
                        }
                    } catch (MqttException e) {
                        e.printStackTrace();
                    }
                }
            });

            // 执行连接
            client.connect(options);

            // 启动后台连接监控
            startConnectionMonitor();

        } catch (Exception e) {
            // ✅ 更新设备状态为异常
            if (iotDeviceService != null){
                IotDevice device = new IotDevice();
                device.setDeviceId(deviceId);
                device.setStatus("ERROR");
                iotDeviceService.updateDeviceStatus(device);
                System.out.println("🔴 [" + protocolName + "] 设备已标记为异常");
            }
            System.err.println("🚫 [" + protocolName + "] 连接异常: " + e.getMessage());
        }
    }

    /**
     * 后台连接状态检测（每10秒检测一次）
     */
    private void startConnectionMonitor() {
        if (running) return;
        running = true;

        Thread monitorThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(10000);
                    if (client == null) continue;

                    if (!client.isConnected()) {
                        System.err.println("⚠️ [" + protocolName + "] 检测到断线，尝试重连...");
                        reconnect();
                    }
                } catch (Exception e) {
                    System.err.println("🚫 [" + protocolName + "] 监控线程异常: " + e.getMessage());
                }
            }
        });

        monitorThread.setDaemon(true);
        monitorThread.start();
    }

    /** 手动重连逻辑 */
    private synchronized void reconnect() {
        try {
            if (client != null && !client.isConnected()) {
                client.reconnect();
                System.out.println("🔁 [" + protocolName + "] 已尝试重新连接");
            }
        } catch (MqttException e) {
            System.err.println("🚫 [" + protocolName + "] 重连失败: " + e.getMessage());
        }
    }

    /** 发布消息 */
    public int publish(String topic, String payload, int qos, boolean retained) {
        try {
            if (client == null || !client.isConnected()) {
                System.err.println("⚠️ [" + protocolName + "] 未连接，无法发布消息");
                return 0;
            }

            if (topic == null || topic.isEmpty()) {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode node = mapper.readTree(configJson);
                topic = node.has("topic") ? node.get("topic").asText() : null;
            }

            if (topic == null || topic.isEmpty()) {
                System.err.println("🚫 [" + protocolName + "] 无效的 topic");
                return 0;
            }

            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(qos);
            message.setRetained(retained);
            client.publish(topic, message);

            System.out.println("📤 [" + protocolName + "] 发布成功 -> topic=" + topic + " payload=" + payload);
            return 1;
        } catch (Exception e) {
            System.err.println("🚫 [" + protocolName + "] 发布异常: " + e.getMessage());
            return 0;
        }
    }

    /** 断开连接 */
    public synchronized void disconnect() {
        running = false;
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
                System.out.println("🔌 [" + protocolName + "] 已断开连接");

                // ✅ 更新设备状态为离线
                if (iotDeviceService != null) {
                    IotDevice device = new IotDevice();
                    device.setDeviceId(deviceId);
                    device.setStatus("OFFLINE");
                    iotDeviceService.updateDeviceStatus(device);
                    System.out.println("🔴 [" + protocolName + "] 设备已标记为离线");
                }
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    /** 保存缓存中的最新数据到数据库 */
    public void saveLatestData() {
        if (messageQueue.isEmpty()) return;

        try {
            List<IotTelemetry> list = new ArrayList<>();
            ObjectMapper mapper = new ObjectMapper();

            while (!messageQueue.isEmpty()) {
                Map<String, Object> record = messageQueue.poll();
                if (record == null) continue;

                IotTelemetry telemetry = new IotTelemetry();
                telemetry.setTelemetryId(SnowflakeIdGenerator.nextId());
                telemetry.setDeviceId(deviceId);

                String json = mapper.writeValueAsString(record);
                telemetry.setDataJson(json);

                list.add(telemetry);
            }

            if (!list.isEmpty()) {
                iotTelemetryService.batchSaveTelemetry(list);
                System.out.println("✅ 批量写入 " + list.size() + " 条 JSON 数据");
            }

        } catch (Exception e) {
            System.err.println("🚫 JSON入库失败: " + e.getMessage());
        }
    }

    public String getProtocolName() {
        return protocolName;
    }

    public MqttClient getClient() {
        return client;
    }

    /**
     * 获取合并后的最新数据（两台雷达数据已合并到 COMBINED_KEY）
     */
    public Map<String, Object> getLatestData() {
        return latestDataMap.get(COMBINED_KEY);
    }

    /**
     * 获取所有设备最新数据
     */
    public static Map<Long, Map<String, Object>> getAllLatestData() {
        return new HashMap<>(latestDataMap); // 返回副本防止修改
    }

    /**
     * 将 OneNET 物模型格式转换为后端所需格式
     * 硬件发送: {"id":"123","params":{"heartbeat":{"value":72},"breath":{"value":18}}}
     * 后端期望: {"heartRate":72,"respiratoryRate":18}
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> convertOneNETFormat(Map<String, Object> rawMap) {
        Map<String, Object> converted = new HashMap<>();

        Object paramsObj = rawMap.get("params");
        if (!(paramsObj instanceof Map)) {
            return rawMap; // 无params字段，直接返回原始数据
        }

        Map<String, Object> params = (Map<String, Object>) paramsObj;

        // 字段名映射规则
        // 心跳呼吸项目: heartbeat→heartRate, breath→respiratoryRate
        // 步态识别项目: v_avg→speedFluctuation, final_display_freq→stepFrequency
        for (Map.Entry<String, Object> entry : params.entrySet()) {
            String key = entry.getKey();
            Object valueObj = entry.getValue();

            if (valueObj instanceof Map) {
                Map<String, Object> valueMap = (Map<String, Object>) valueObj;
                Object value = valueMap.get("value");

                // 字段名映射
                String mappedKey = mapFieldName(key);
                if (mappedKey != null) {
                    // speedFluctuation取绝对值（正负是方向，不代表数值大小）
                    if ("speedFluctuation".equals(mappedKey) && value instanceof Number) {
                        value = Math.abs(((Number) value).doubleValue());
                    }
                    converted.put(mappedKey, value);
                } else {
                    // 未映射的字段保留原名
                    converted.put(key, value);
                }
            }
        }

        return converted;
    }

    /**
     * 字段名映射
     */
    private String mapFieldName(String original) {
        switch (original) {
            // 心率呼吸雷达 - 新短字段名
            case "he":
                return "heartRate";
            case "b":
                return "respiratoryRate";
            // 心率呼吸雷达 - 旧字段名（兼容）
            case "heartbeat":
                return "heartRate";
            case "breath":
                return "respiratoryRate";
            // 步态分析雷达
            case "v_avg":
                return "speedFluctuation";
            case "final_display_freq":
                return "stepFrequency";
            case "v_std":
                return "vStd";
            case "current_diff":
                return "currentDiff";
            // 环境传感器
            case "t":
                return "ambientTemperature";
            case "p":
                return "ambientPressure";
            case "h":
                return "ambientHumidity";
            case "m":
                return "soundIntensity";
            default:
                return null; // 不认识的字段不映射
        }
    }
}
