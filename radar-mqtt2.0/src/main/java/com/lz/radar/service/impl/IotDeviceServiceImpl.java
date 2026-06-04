package com.lz.radar.service.impl;

import com.lz.radar.domain.IotDevice;
import com.lz.radar.domain.IotProtocol;
import com.lz.radar.domain.dto.DeviceProtocolDto;
import com.lz.radar.mapper.IotDeviceMapper;
import com.lz.radar.service.IotDeviceService;
import com.lz.radar.service.IotProtocolService;
import com.lz.radar.utils.SnowflakeIdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * IoT设备信息服务实现类
 * 实现设备相关的业务逻辑
 */
@Service
public class IotDeviceServiceImpl implements IotDeviceService {

    private static final Logger logger = LoggerFactory.getLogger(IotDeviceServiceImpl.class);

    @Autowired
    private IotDeviceMapper deviceMapper;

    @Autowired
    private IotProtocolService protocolService;

    /**
     * 获取所有设备列表
     * @return 设备列表
     */
    @Override
    public List<IotDevice> getAllDevices() {
        List<IotDevice> iotDevices = deviceMapper.selectAllDevices();

        for (IotDevice iotDevice : iotDevices) {
            try {
                // 统一设置默认值
                iotDevice.setHost("");
                iotDevice.setTopic("");

                // 只有协议ID不为空时才尝试获取协议配置
                if (iotDevice.getProtocolId() != null) {
                    IotProtocol protocol = protocolService.getProtocolById(iotDevice.getProtocolId());

                    if (protocol != null && protocol.getProtocolConfig() != null) {
                        // 安全获取host和topic，处理可能的空值
                        String host = protocol.getProtocolConfig().getHost();
                        String topic = protocol.getProtocolConfig().getTopic();

                        iotDevice.setHost(host != null ? host : "");
                        iotDevice.setTopic(topic != null ? topic : "");
                    } else {
                         //协议不存在或配置为空，记录日志
                        logger.warn("设备 {} 的协议不存在或配置为空，协议ID: {}",
                                iotDevice.getDeviceId(), iotDevice.getProtocolId());
                    }
                }
            } catch (Exception e) {
                // 异常处理，确保单条设备处理失败不影响整个列表
                logger.error("处理设备 {} 的协议配置时发生错误: {}",
                        iotDevice.getDeviceId(), e.getMessage());
                // 保持默认的空值
            }
        }
        return iotDevices;
    }

    /**
     * 根据设备ID获取设备信息
     * @param deviceId 设备ID
     * @return 设备信息
     */
    @Override
    public IotDevice getDeviceById(Long deviceId) {
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("设备ID不能为空且必须大于0");
        }
        try {
            IotDevice device = deviceMapper.selectDeviceById(deviceId);
            if (device == null) {
                throw new RuntimeException("设备不存在，设备ID: " + deviceId);
            }
            return device;
        } catch (Exception e) {
            throw new RuntimeException("获取设备信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据设备编码获取设备信息
     * @param deviceCode 设备编码
     * @return 设备信息
     */
    @Override
    public IotDevice getDeviceByCode(String deviceCode) {
        if (deviceCode == null || deviceCode.trim().isEmpty()) {
            throw new IllegalArgumentException("设备编码不能为空");
        }
        try {
            IotDevice device = deviceMapper.selectDeviceByCode(deviceCode.trim());
            if (device == null) {
                throw new RuntimeException("设备不存在，设备编码: " + deviceCode);
            }
            return device;
        } catch (Exception e) {
            throw new RuntimeException("获取设备信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据设备状态获取设备列表
     * @param status 设备状态
     * @return 设备列表
     */
    @Override
    public List<IotDevice> getDevicesByStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            throw new IllegalArgumentException("设备状态不能为空");
        }
        try {
            return deviceMapper.selectDevicesByStatus(status.trim());
        } catch (Exception e) {
            throw new RuntimeException("根据状态获取设备列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 添加新设备
     * @param device 设备信息
     * @return 是否添加成功
     */
    @Override
    @Transactional
    public boolean addDevice(IotDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("设备信息不能为空");
        }
        if (device.getDeviceName() == null || device.getDeviceName().trim().isEmpty()) {
            throw new IllegalArgumentException("设备名称不能为空");
        }
        if (device.getDeviceCode() == null || device.getDeviceCode().trim().isEmpty()) {
            throw new IllegalArgumentException("设备编码不能为空");
        }
        
        try {
            // 检查设备编码是否已存在
            IotDevice existingDevice = deviceMapper.selectDeviceByCode(device.getDeviceCode());
            if (existingDevice != null) {
                throw new RuntimeException("设备编码已存在: " + device.getDeviceCode());
            }

            device.setDeviceId(SnowflakeIdGenerator.nextId());
            int result = deviceMapper.insertDevice(device);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("添加设备失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新设备信息
     * @param device 设备信息
     * @return 是否更新成功
     */
    @Override
    @Transactional
    public boolean updateDevice(IotDevice device) {
        if (device == null || device.getDeviceId() == null) {
            throw new IllegalArgumentException("设备信息和设备ID不能为空");
        }
        
        try {
            // 检查设备是否存在
            IotDevice existingDevice = deviceMapper.selectDeviceById(device.getDeviceId());
            if (existingDevice == null) {
                throw new RuntimeException("设备不存在，设备ID: " + device.getDeviceId());
            }
            
            int result = deviceMapper.updateDevice(device);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("更新设备信息失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新设备状态
     * @return 是否更新成功
     */
    @Override
    @Transactional
    public boolean updateDeviceStatus(IotDevice device) {
        if (device.getDeviceId() == null || device.getDeviceId() <= 0) {
            throw new IllegalArgumentException("设备ID不能为空且必须大于0");
        }
        if (device.getStatus() == null || device.getStatus().trim().isEmpty()) {
            throw new IllegalArgumentException("设备状态不能为空");
        }
        
        try {
            // 检查设备是否存在
            IotDevice existingDevice = deviceMapper.selectDeviceById(device.getDeviceId());
            if (existingDevice == null) {
                throw new RuntimeException("设备不存在，设备ID: " + device.getDeviceId());
            }
            
            int result = deviceMapper.updateDeviceStatus(device);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("更新设备状态失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除设备
     * @param deviceId 设备ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteDevice(Long deviceId) {
        if (deviceId == null || deviceId <= 0) {
            throw new IllegalArgumentException("设备ID不能为空且必须大于0");
        }
        
        try {
            // 检查设备是否存在
            IotDevice existingDevice = deviceMapper.selectDeviceById(deviceId);
            if (existingDevice == null) {
                throw new RuntimeException("设备不存在，设备ID: " + deviceId);
            }
            
            int result = deviceMapper.deleteDeviceById(deviceId);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除设备失败: " + e.getMessage(), e);
        }
    }

    /**
     * 获取设备总数
     * @return 设备总数
     */
    @Override
    public int getDeviceCount() {
        try {
            return deviceMapper.countDevices();
        } catch (Exception e) {
            throw new RuntimeException("获取设备总数失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据协议ID获取设备列表
     * @param protocolId 协议ID
     * @return 设备列表
     */
    @Override
    public List<IotDevice> getDevicesByProtocolId(Long protocolId) {
        if (protocolId == null || protocolId <= 0) {
            throw new IllegalArgumentException("协议ID不能为空且必须大于0");
        }
        try {
            return deviceMapper.selectDevicesByProtocolId(protocolId);
        } catch (Exception e) {
            throw new RuntimeException("根据协议ID获取设备列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新设备协议
     * @param dto 设备协议信息
     * @return 是否更新成功
     */
    @Override
    public boolean updateDeviceProtocol(DeviceProtocolDto dto) {
        if (dto.getDeviceId() == null || dto.getDeviceId() <= 0) {
            throw new IllegalArgumentException("设备ID不能为空且必须大于0");
        }
        if (dto.getProtocolId() == null) {
            throw new IllegalArgumentException("设备协议不能为空");
        }

        try {
            // 检查设备是否存在
            IotDevice existingDevice = deviceMapper.selectDeviceById(dto.getDeviceId());
            if (existingDevice == null) {
                throw new RuntimeException("设备不存在，设备ID: " + dto.getDeviceId());
            }

            int result = deviceMapper.updateDeviceProtocol(dto);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("更新设备协议失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据协议ID获取设备列表
     * @param protocolId 协议ID
     * @return 设备列表
     */
    @Override
    public IotDevice getDevicesByProtocolIdOrStatus(Long protocolId) {
        if (protocolId == null || protocolId <= 0) {
            throw new IllegalArgumentException("协议ID不能为空且必须大于0");
        }
        try {
            return deviceMapper.getDevicesByProtocolIdOrStatus(protocolId);
        } catch (Exception e) {
            throw new RuntimeException("根据协议ID获取设备列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据继电器协议ID获取设备
     * @param protocolId 协议ID
     * @return 设备列表
     */
    @Override
    public IotDevice selectDeviceByRelayId(Long protocolId) {
        if (protocolId == null || protocolId <= 0) {
            throw new IllegalArgumentException("继电器协议ID不能为空且必须大于0");
        }
        try {
            return deviceMapper.selectDeviceByRelayId(protocolId);
        } catch (Exception e) {
            throw new RuntimeException("根据继电器协议ID获取设备列表失败: " + e.getMessage(), e);
        }
    }
}