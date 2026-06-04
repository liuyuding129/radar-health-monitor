package com.lz.radar.mapper;

import com.lz.radar.domain.IotDevice;
import com.lz.radar.domain.dto.DeviceProtocolDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * IoT设备信息数据访问层接口
 * 使用MyBatis注解方式实现SQL映射
 */
@Mapper
public interface IotDeviceMapper {
    
    /**
     * 查询所有设备信息
     * @return 设备列表
     */
    List<IotDevice> selectAllDevices();
    
    /**
     * 根据设备ID查询设备信息
     * @param deviceId 设备ID
     * @return 设备信息
     */
    IotDevice selectDeviceById(@Param("deviceId") Long deviceId);
    
    /**
     * 根据设备编码查询设备信息
     * @param deviceCode 设备编码
     * @return 设备信息
     */
    IotDevice selectDeviceByCode(@Param("deviceCode") String deviceCode);
    
    /**
     * 根据设备状态查询设备列表
     * @param status 设备状态
     * @return 设备列表
     */
    List<IotDevice> selectDevicesByStatus(@Param("status") String status);
    
    /**
     * 插入新设备信息
     * @param device 设备实体对象
     * @return 插入记录数
     */
    int insertDevice(IotDevice device);
    
    /**
     * 更新设备信息
     * @param device 设备实体对象
     * @return 更新记录数
     */
    int updateDevice(IotDevice device);
    
    /**
     * 更新设备状态
     * @param device 设备实体对象
     * @return 更新记录数
     */
    int updateDeviceStatus(IotDevice device);
    
    /**
     * 根据设备ID删除设备
     * @param deviceId 设备ID
     * @return 删除记录数
     */
    int deleteDeviceById(@Param("deviceId") Long deviceId);
    
    /**
     * 统计设备数量
     * @return 设备总数
     */
    int countDevices();
    
    /**
     * 根据协议ID查询使用该协议的设备列表
     * @param protocolId 协议ID
     * @return 设备列表
     */
    List<IotDevice> selectDevicesByProtocolId(@Param("protocolId") Long protocolId);

    /**
     * 更新设备协议
     * @param dto 设备协议信息
     * @return 更新记录数
     */
    int updateDeviceProtocol(DeviceProtocolDto dto);

    /**
     * 根据协议ID或设备状态查询设备列表
     * @param protocolId 协议ID
     * @return 设备列表
     */
    IotDevice getDevicesByProtocolIdOrStatus(Long protocolId);

    /**
     * 根据继电器ID查询设备信息
     * @param protocolId 继电器ID
     * @return 设备信息
     */
    IotDevice selectDeviceByRelayId(Long protocolId);
}