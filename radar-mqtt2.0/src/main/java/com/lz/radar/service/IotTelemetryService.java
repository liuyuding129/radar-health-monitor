package com.lz.radar.service;


import com.lz.radar.domain.IotMessage;
import com.lz.radar.domain.IotTelemetry;
import com.lz.radar.domain.dto.IotTelemetryDTO;

import java.util.List;

/**
 * 设备时序数据服务接口
 * 定义时序数据相关的业务逻辑方法
 */
public interface IotTelemetryService {
    
    /**
     * 获取所有时序数据
     * @return 时序数据列表
     */
    List<IotTelemetry> getAllTelemetry();
    
    /**
     * 根据设备ID获取时序数据
     * @param deviceId 设备ID
     * @return 时序数据列表
     */
    List<IotTelemetry> getTelemetryByDeviceId(Long deviceId);
    
    /**
     * 根据数据键名获取时序数据
     * @param dataKey 数据键名
     * @return 时序数据列表
     */
    List<IotTelemetry> getTelemetryByDataKey(String dataKey);
    
    /**
     * 保存时序数据记录
     * @param telemetry 时序数据信息
     * @return 是否保存成功
     */
    boolean saveTelemetry(IotTelemetry telemetry);
    
    /**
     * 批量保存时序数据记录
     * @param telemetryList 时序数据列表
     * @return 是否保存成功
     */
    boolean batchSaveTelemetry(List<IotTelemetry> telemetryList);
    
    /**
     * 根据时间范围查询时序数据
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时序数据列表
     */
    List<IotTelemetry> getTelemetryByTimeRange(Long deviceId, String startTime, String endTime);
    
    /**
     * 获取设备的最新数据
     * @param deviceId 设备ID
     * @return 最新时序数据列表
     */
    List<IotTelemetry> getLatestTelemetryByDeviceId(Long deviceId);
    
    /**
     * 解析消息并保存时序数据
     * @param message 原始消息
     * @return 是否解析成功
     */
    boolean parseAndSaveTelemetry(IotMessage message);

    IotTelemetryDTO selectDeviceOrTelemetryData();
}