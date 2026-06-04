package com.lz.radar.service;


import com.lz.radar.domain.IotDevice;
import com.lz.radar.domain.dto.DeviceProtocolDto;

import java.util.List;

/**
 * IoT设备信息服务接口
 * 定义设备相关的业务逻辑方法
 */
public interface IotDeviceService {
    
    /**
     * 获取所有设备列表
     * @return 设备列表
     */
    List<IotDevice> getAllDevices();
    
    /**
     * 根据设备ID获取设备信息
     * @param deviceId 设备ID
     * @return 设备信息
     */
    IotDevice getDeviceById(Long deviceId);
    
    /**
     * 根据设备编码获取设备信息
     * @param deviceCode 设备编码
     * @return 设备信息
     */
    IotDevice getDeviceByCode(String deviceCode);
    
    /**
     * 根据设备状态获取设备列表
     * @param status 设备状态
     * @return 设备列表
     */
    List<IotDevice> getDevicesByStatus(String status);
    
    /**
     * 添加新设备
     * @param device 设备信息
     * @return 是否添加成功
     */
    boolean addDevice(IotDevice device);
    
    /**
     * 更新设备信息
     * @param device 设备信息
     * @return 是否更新成功
     */
    boolean updateDevice(IotDevice device);
    
    /**
     * 更新设备状态
     * @param deviceId 设备ID
     * @param status 设备状态
     * @return 是否更新成功
     */
    boolean updateDeviceStatus(IotDevice device);
    
    /**
     * 删除设备
     * @param deviceId 设备ID
     * @return 是否删除成功
     */
    boolean deleteDevice(Long deviceId);
    
    /**
     * 获取设备总数
     * @return 设备总数
     */
    int getDeviceCount();
    
    /**
     * 根据协议ID获取设备列表
     * @param protocolId 协议ID
     * @return 设备列表
     */
    List<IotDevice> getDevicesByProtocolId(Long protocolId);

    /**
     * 更新设备协议
     * @param dto 设备协议信息
     * @return 是否更新成功
     */
    boolean updateDeviceProtocol(DeviceProtocolDto dto);

    /**
     * 根据协议ID或设备状态获取设备列表
     * @param protocolId 协议ID
     * @return 设备列表
     */
    IotDevice getDevicesByProtocolIdOrStatus(Long protocolId);

    /**
     * 根据继电器协议ID获取设备
     * @param protocolId 协议ID
     * @return 设备
     */
    IotDevice selectDeviceByRelayId(Long protocolId);
}