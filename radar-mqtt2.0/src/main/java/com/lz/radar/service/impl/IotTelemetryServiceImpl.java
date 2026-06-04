package com.lz.radar.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lz.radar.configuration.MqttConnection;
import com.lz.radar.domain.IotDevice;
import com.lz.radar.domain.IotMessage;
import com.lz.radar.domain.IotTelemetry;
import com.lz.radar.domain.dto.IotTelemetryDTO;
import com.lz.radar.mapper.IotDeviceMapper;
import com.lz.radar.mapper.IotTelemetryMapper;
import com.lz.radar.service.IotTelemetryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 设备时序数据服务实现类
 * 实现时序数据相关的业务逻辑
 */
@Service
public class IotTelemetryServiceImpl implements IotTelemetryService {

    private static final Logger logger = LoggerFactory.getLogger(IotTelemetryServiceImpl.class);
    
    @Autowired
    private IotTelemetryMapper telemetryMapper;
    
    @Autowired
    private IotDeviceMapper deviceMapper;
    
    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 获取所有时序数据
     * @return 时序数据列表
     */
    @Override
    public List<IotTelemetry> getAllTelemetry() {
        try {
            return telemetryMapper.selectAllTelemetry();
        } catch (Exception e) {
            logger.error("获取时序数据列表失败", e);
            throw new RuntimeException("获取时序数据列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据设备ID获取时序数据
     * @param deviceId 设备ID
     * @return 时序数据列表
     */
    @Override
    public List<IotTelemetry> getTelemetryByDeviceId(Long deviceId) {
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("设备ID不能为空且必须大于0");
        }
        try {
            List<IotTelemetry> iotTelemetries = telemetryMapper.selectTelemetryByDeviceId(deviceId);
            return iotTelemetries;
        } catch (Exception e) {
            logger.error("根据设备ID获取时序数据失败，设备ID: {}", deviceId, e);
            throw new RuntimeException("根据设备ID获取时序数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据数据键名获取时序数据
     * @param dataKey 数据键名
     * @return 时序数据列表
     */
    @Override
    public List<IotTelemetry> getTelemetryByDataKey(String dataKey) {
        if (dataKey == null || dataKey.trim().isEmpty()) {
            throw new IllegalArgumentException("数据键名不能为空");
        }
        try {
            return telemetryMapper.selectTelemetryByDataKey(dataKey.trim());
        } catch (Exception e) {
            logger.error("根据数据键名获取时序数据失败，数据键名: {}", dataKey, e);
            throw new RuntimeException("根据数据键名获取时序数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 保存时序数据记录
     * @param telemetry 时序数据信息
     * @return 是否保存成功
     */
    @Override
    @Transactional
    public boolean saveTelemetry(IotTelemetry telemetry) {
        if (telemetry == null) {
            throw new IllegalArgumentException("时序数据信息不能为空");
        }
        if (telemetry.getDeviceId() == null || telemetry.getDeviceId() <= 0) {
            throw new IllegalArgumentException("设备ID不能为空且必须大于0");
        }
        if (telemetry.getDataJson() == null || telemetry.getDataJson().trim().isEmpty()) {
            throw new IllegalArgumentException("数据源不能为空");
        }
        
        try {
            // 检查设备是否存在
            IotDevice device = deviceMapper.selectDeviceById(telemetry.getDeviceId());
            if (device == null) {
                throw new RuntimeException("设备不存在，设备ID: " + telemetry.getDeviceId());
            }
            
            int result = telemetryMapper.insertTelemetry(telemetry);
            boolean success = result > 0;
            
            if (success) {
                logger.debug("成功保存时序数据记录，数据ID: {}, 设备ID: {}, 数据源: {}",
                           telemetry.getTelemetryId(), telemetry.getDeviceId(), telemetry.getDataJson());
            }
            
            return success;
        } catch (Exception e) {
            logger.error("保存时序数据记录失败，设备ID: {}, 数据源: {}", telemetry.getDeviceId(), telemetry.getDataJson(), e);
            throw new RuntimeException("保存时序数据记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量保存时序数据记录
     * @param telemetryList 时序数据列表
     * @return 是否保存成功
     */
    @Override
    @Transactional
    public boolean batchSaveTelemetry(List<IotTelemetry> telemetryList) {
        if (telemetryList == null || telemetryList.isEmpty()) {
            throw new IllegalArgumentException("时序数据列表不能为空");
        }
        
        try {
            // 验证所有设备ID和数据键名
            for (IotTelemetry telemetry : telemetryList) {
                if (telemetry.getDeviceId() == null || telemetry.getDeviceId() <= 0) {
                    throw new IllegalArgumentException("设备ID不能为空且必须大于0");
                }
                if (telemetry.getDataJson() == null || telemetry.getDataJson().trim().isEmpty()) {
                    throw new IllegalArgumentException("数据源不能为空");
                }

            }
            
            int result = telemetryMapper.batchInsertTelemetry(telemetryList);
            boolean success = result > 0;
            
            if (success) {
                logger.info("批量保存时序数据记录成功，共 {} 条记录", telemetryList.size());
            }
            
            return success;
        } catch (Exception e) {
            logger.error("批量保存时序数据记录失败", e);
            throw new RuntimeException("批量保存时序数据记录失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据时间范围查询时序数据
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时序数据列表
     */
    @Override
    public List<IotTelemetry> getTelemetryByTimeRange(Long deviceId, String startTime, String endTime) {
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("设备ID不能为空且必须大于0");
        }
        if (startTime == null || startTime.trim().isEmpty()) {
            throw new IllegalArgumentException("开始时间不能为空");
        }
        if (endTime == null || endTime.trim().isEmpty()) {
            throw new IllegalArgumentException("结束时间不能为空");
        }
        
        try {
            return telemetryMapper.selectTelemetryByTimeRange(deviceId, startTime.trim(), endTime.trim());
        } catch (Exception e) {
            logger.error("根据时间范围查询时序数据失败，设备ID: {}, 时间范围: {} - {}", deviceId, startTime, endTime, e);
            throw new RuntimeException("根据时间范围查询时序数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取设备的最新数据
     * @param deviceId 设备ID
     * @return 最新时序数据列表
     */
    @Override
    public List<IotTelemetry> getLatestTelemetryByDeviceId(Long deviceId) {
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("设备ID不能为空且必须大于0");
        }
        try {
            return telemetryMapper.selectLatestTelemetryByDeviceId(deviceId);
        } catch (Exception e) {
            logger.error("获取设备最新数据失败，设备ID: {}", deviceId, e);
            throw new RuntimeException("获取设备最新数据失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解析消息并保存时序数据
     * @param message 原始消息
     * @return 是否解析成功
     */
    @Override
    @Transactional
    public boolean parseAndSaveTelemetry(IotMessage message) {

        if (message == null) {
            throw new IllegalArgumentException("消息不能为空");
        }

        if (message.getPayload() == null || message.getPayload().trim().isEmpty()) {
            logger.warn("消息内容为空，无法保存，消息ID: {}", message.getMsgId());
            return false;
        }

        try {
            String payload = message.getPayload().trim();

            IotTelemetry telemetry = new IotTelemetry();

            // ✅ 基础字段
            telemetry.setTelemetryId(com.lz.radar.utils.SnowflakeIdGenerator.nextId());
            telemetry.setDeviceId(message.getDeviceId());

            // ✅ 核心：直接存 JSON（不解析）
            telemetry.setDataJson(payload);

            // ✅ 直接保存
            int result = telemetryMapper.insertTelemetry(telemetry);

            if (result > 0) {
                logger.info("✅ 保存成功，deviceId: {}, data: {}",
                        message.getDeviceId(), payload);
                return true;
            }

            return false;

        } catch (Exception e) {
            logger.error("🚫 保存失败，消息ID: {}", message.getMsgId(), e);
            throw new RuntimeException("保存失败: " + e.getMessage(), e);
        }
    }

    @Override
    public IotTelemetryDTO selectDeviceOrTelemetryData() {

//        List<IotTelemetry> telemetry = telemetryMapper.selectDeviceOrTelemetryData();
//
//        return convertToDTO(originalData, null, deviceId);
        return null;
    }

    /**
     * 将 Map 数据转换为 DTO 对象
     */
    private IotTelemetryDTO convertToDTO(Map<String, Object> dataMap, LocalDateTime createTime, Long deviceId) {
        IotTelemetryDTO dto = new IotTelemetryDTO();

        // 获取数值并添加单位
        dto.setHeartRate(addUnit(getStringValue(dataMap, "heartRate"), "bpm"));
        dto.setRespiratoryRate(addUnit(getStringValue(dataMap, "respiratoryRate"), "次/分"));
        dto.setAmbientPressure(addUnit(getStringValue(dataMap, "ambientPressure"), "hPa"));
        dto.setAmbientHumidity(addUnit(getStringValue(dataMap, "ambientHumidity"), "%"));
        dto.setAmbientTemperature(addUnit(getStringValue(dataMap, "ambientTemperature"), "℃"));
        dto.setStepFrequency(addUnit(getStringValue(dataMap, "stepFrequency"), "步/分"));
        dto.setSpeedFluctuation(addUnit(getStringValue(dataMap, "speedFluctuation"), "m/s²"));
        dto.setSoundIntensity(addUnit(getStringValue(dataMap, "soundIntensity"), "dB"));

        return dto;
    }

    private String getStringValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) {
            return null;
        }
        return value instanceof String ? (String) value : value.toString();
    }

    private String addUnit(String value, String unit) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        return value + " " + unit;
    }
}