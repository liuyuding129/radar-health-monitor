package com.lz.radar.configuration;

import com.lz.radar.domain.IotProtocol;
import com.lz.radar.service.IotDeviceService;
import com.lz.radar.service.IotTelemetryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 管理多个 MQTT 连接实例
 */
@Component
public class MqttManager {

    // 存放每个协议对应的连接对象（key = protocolId）
    private final Map<Long, MqttConnection> connectionMap = new HashMap<>();

    // 存放每个协议的配置哈希，判断是否变化
    private final Map<Long, String> configHashMap = new HashMap<>();

    /** 存放每个协议对应的设备ID（方便后续入库关联） */
    private final Map<Long, Long> protocolDeviceMap = new HashMap<>();

    @Autowired
    private IotTelemetryService iotTelemetryService;

    @Autowired
    private IotDeviceService iotDeviceService;

    /**
     * 根据数据库配置初始化所有连接
     */
    public synchronized void initializeConnections(List<IotProtocol> configs) {
        for (IotProtocol config : configs) {
            Long protocolId = config.getProtocolId();
            Long deviceId = config.getDeviceId(); // ✅ 从 JOIN 查询得到
            String newHash = String.valueOf(config.getConfigJson().hashCode());
            String oldHash = configHashMap.get(protocolId);

            // 保存设备映射
            protocolDeviceMap.put(protocolId, deviceId);

            // 判断配置是否变化
            if (!newHash.equals(oldHash)) {
                System.out.println("🔄 [" + config.getProtocolName() + "] 配置变化，准备重连...");

                // 断开旧连接
                disconnect(protocolId);

                // 新建连接实例（带设备ID）
                MqttConnection connection = new MqttConnection(
                        config.getProtocolName(),
                        config.getConfigJson(),
                        deviceId,  // ✅ 将设备ID传入连接
                        protocolId,
                        iotTelemetryService,
                        iotDeviceService
                );

                // 建立连接
                connection.connect();

                // 更新缓存映射
                connectionMap.put(protocolId, connection);
                configHashMap.put(protocolId, newHash);
            }
        }
    }

    /**
     * 每分钟保存所有连接的最新数据
     */
    @Scheduled(fixedRate = 5000)
    public void saveAllLatestData() {
        for (Map.Entry<Long, MqttConnection> entry : connectionMap.entrySet()) {
            Long protocolId = entry.getKey();
            MqttConnection conn = entry.getValue();

            try {
                // 1. 保存队列中的批量数据
                conn.saveLatestData();

            } catch (Exception e) {
                System.err.println("🚫 保存数据失败 [protocolId=" + protocolId + "]: " + e.getMessage());
            }
        }
    }

    /**
     * 发布消息（支持广播）
     */
    public int publish(String protocolName, String topic, String payload, int qos, boolean retained) {
        if (connectionMap.isEmpty()) {
            System.err.println("⚠️ 当前没有任何已连接的 MQTT 客户端");
            return 0;
        }

        List<MqttConnection> targets = new ArrayList<>();
        if (protocolName == null || protocolName.isEmpty()) {
            targets.addAll(connectionMap.values());
        } else {
            for (MqttConnection conn : connectionMap.values()) {
                if (protocolName.equalsIgnoreCase(conn.getProtocolName())) {
                    targets.add(conn);
                }
            }
        }

        if (targets.isEmpty()) {
            System.err.println("🚫 未找到匹配的协议连接：" + protocolName);
            return 0;
        }

        for (MqttConnection conn : targets) {
            int publish = conn.publish(topic, payload, qos, retained);

            return publish;
        }
        return 1;
    }

    /**
     * 断开单个连接
     */
    private void disconnect(Long protocolId) {
        if (connectionMap.containsKey(protocolId)) {
            connectionMap.get(protocolId).disconnect();
            connectionMap.remove(protocolId);
        }
    }

}
