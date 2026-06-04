package com.lz.radar.mapper;

import com.lz.radar.domain.IotTelemetry;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备时序数据数据访问层接口
 */
@Mapper
public interface IotTelemetryMapper {
    
    /**
     * 查询所有时序数据
     * @return 时序数据列表
     */
    List<IotTelemetry> selectAllTelemetry();
    
    /**
     * 根据设备ID查询时序数据
     * @param deviceId 设备ID
     * @return 时序数据列表
     */
    List<IotTelemetry> selectTelemetryByDeviceId(@Param("deviceId") Long deviceId);
    
    /**
     * 根据数据键名查询时序数据
     * @param dataKey 数据键名
     * @return 时序数据列表
     */
    List<IotTelemetry> selectTelemetryByDataKey(@Param("dataKey") String dataKey);
    
    /**
     * 插入新时序数据记录
     * @param telemetry 时序数据实体对象
     * @return 插入记录数
     */
    int insertTelemetry(IotTelemetry telemetry);
    
    /**
     * 批量插入时序数据记录
     * @param telemetryList 时序数据列表
     * @return 插入记录数
     */
    int batchInsertTelemetry(@Param("telemetryList") List<IotTelemetry> telemetryList);
    
    /**
     * 根据设备ID和时间范围查询时序数据
     * @param deviceId 设备ID
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 时序数据列表
     */
    List<IotTelemetry> selectTelemetryByTimeRange(@Param("deviceId") Long deviceId,
                                                 @Param("startTime") String startTime, 
                                                 @Param("endTime") String endTime);
    
    /**
     * 根据设备ID删除时序数据
     * @param deviceId 设备ID
     * @return 删除记录数
     */
    int deleteTelemetryByDeviceId(@Param("deviceId") Integer deviceId);
    
    /**
     * 获取设备的最新数据
     * @param deviceId 设备ID
     * @return 最新时序数据列表
     */
    List<IotTelemetry> selectLatestTelemetryByDeviceId(@Param("deviceId") Long deviceId);

}