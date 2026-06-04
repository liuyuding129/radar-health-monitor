package com.lz.radar.service.impl;

import com.alibaba.fastjson2.JSON;
import com.lz.radar.domain.IotProtocol;
import com.lz.radar.domain.dto.ProtocolConfigDto;
import com.lz.radar.mapper.IotProtocolMapper;
import com.lz.radar.service.IotProtocolService;
import com.lz.radar.utils.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 通信协议配置服务实现类
 * 实现协议相关的业务逻辑
 */
@Service
public class IotProtocolServiceImpl implements IotProtocolService {

    @Autowired
    private IotProtocolMapper protocolMapper;

    /**
     * 获取所有协议配置
     * @return 协议配置列表
     */
    @Override
    public List<IotProtocol> getAllProtocols(String protocolType) {
        try {
            List<IotProtocol> iotProtocols = protocolMapper.selectAllProtocols(protocolType);

            for (IotProtocol protocol : iotProtocols) {
                protocol.setProtocolConfig(JSON.parseObject(protocol.getConfigJson(), ProtocolConfigDto.class));
            }
            return iotProtocols;
        } catch (Exception e) {
            throw new RuntimeException("获取协议配置列表失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据协议ID获取协议配置
     * @param protocolId 协议ID
     * @return 协议配置信息
     */
    @Override
    public IotProtocol getProtocolById(Long protocolId) {
        if (protocolId == null || protocolId <= 0) {
            throw new IllegalArgumentException("协议ID不能为空且必须大于0");
        }
        try {

            IotProtocol protocol = protocolMapper.selectProtocolById(protocolId);
            if (protocol == null) {
                throw new RuntimeException("协议配置不存在，协议ID: " + protocolId);
            }

            if (protocol.getProtocolId() == null){
                protocol.setProtocolConfig(new ProtocolConfigDto());
            }
            protocol.setProtocolConfig(JSON.parseObject(protocol.getConfigJson(), ProtocolConfigDto.class));

            return protocol;
        } catch (Exception e) {
            throw new RuntimeException("获取协议配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 根据协议名称获取协议配置
     * @param protocolName 协议名称
     * @return 协议配置信息
     */
    @Override
    public IotProtocol getProtocolByName(String protocolName) {
        if (protocolName == null || protocolName.trim().isEmpty()) {
            throw new IllegalArgumentException("协议名称不能为空");
        }
        try {
            IotProtocol protocol = protocolMapper.selectProtocolByName(protocolName.trim());
            protocol.setProtocolConfig(JSON.parseObject(protocol.getConfigJson(), ProtocolConfigDto.class));
            if (protocol == null) {
                throw new RuntimeException("协议配置不存在，协议名称: " + protocolName);
            }
            return protocol;
        } catch (Exception e) {
            throw new RuntimeException("获取协议配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 添加新协议配置
     * @param protocol 协议配置信息
     * @return 是否添加成功
     */
    @Override
    @Transactional
    public boolean addProtocol(IotProtocol protocol) {
        if (protocol == null) {
            throw new IllegalArgumentException("协议配置信息不能为空");
        }
        if (protocol.getProtocolName() == null || protocol.getProtocolName().trim().isEmpty()) {
            throw new IllegalArgumentException("协议名称不能为空");
        }
        
        try {
            // 检查协议名称是否已存在
            IotProtocol existingProtocol = protocolMapper.selectProtocolByName(protocol.getProtocolName());
            if (existingProtocol != null) {
                throw new RuntimeException("协议名称已存在: " + protocol.getProtocolName());
            }
            protocol.setProtocolId(SnowflakeIdGenerator.nextId());
            protocol.setConfigJson(JSON.toJSONString(protocol.getProtocolConfig()));
            int result = protocolMapper.insertProtocol(protocol);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("添加协议配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 更新协议配置
     * @param protocol 协议配置信息
     * @return 是否更新成功
     */
    @Override
    @Transactional
    public boolean updateProtocol(IotProtocol protocol) {
        if (protocol == null || protocol.getProtocolId() == null) {
            throw new IllegalArgumentException("协议配置信息和协议ID不能为空");
        }
        
        try {
            // 检查协议配置是否存在
            IotProtocol existingProtocol = protocolMapper.selectProtocolById(protocol.getProtocolId());
            if (existingProtocol == null) {
                throw new RuntimeException("协议配置不存在，协议ID: " + protocol.getProtocolId());
            }
            protocol.setConfigJson(JSON.toJSONString(protocol.getProtocolConfig()));
            int result = protocolMapper.updateProtocol(protocol);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("更新协议配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 删除协议配置
     * @param protocolId 协议ID
     * @return 是否删除成功
     */
    @Override
    @Transactional
    public boolean deleteProtocol(Long protocolId) {
        if (protocolId == null || protocolId <= 0) {
            throw new IllegalArgumentException("协议ID不能为空且必须大于0");
        }
        
        try {
            // 检查协议配置是否存在
            IotProtocol existingProtocol = protocolMapper.selectProtocolById(protocolId);
            if (existingProtocol == null) {
                throw new RuntimeException("协议配置不存在，协议ID: " + protocolId);
            }
            
            int result = protocolMapper.deleteProtocolById(protocolId);
            return result > 0;
        } catch (Exception e) {
            throw new RuntimeException("删除协议配置失败: " + e.getMessage(), e);
        }
    }

    /**
     * 验证协议配置是否有效
     * @param protocol 协议配置
     * @return 是否有效
     */
    @Override
    public boolean validateProtocolConfig(IotProtocol protocol) {
        if (protocol == null) {
            return false;
        }
        
        // 验证协议名称
        if (protocol.getProtocolName() == null || protocol.getProtocolName().trim().isEmpty()) {
            return false;
        }
        
        // 验证配置JSON
        if (protocol.getProtocolConfig() == null) {
            return false;
        }
        
        try {
            // 这里可以添加更复杂的JSON格式验证
            // 例如使用JSON解析器验证JSON格式是否正确
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 根据协议类型获取协议配置列表
     * @return 协议配置列表
     */
    @Override
    public List<IotProtocol> getProtocolsByMQTT() {
        try {
            List<IotProtocol> protocol = protocolMapper.getProtocolsByMQTT();
            if (protocol == null) {
                throw new RuntimeException("协议配置不存在");
            }
            return protocol;
        } catch (Exception e) {
            throw new RuntimeException("获取协议配置失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<IotProtocol> getProtocolsByTCP() {
        try {
            List<IotProtocol> protocol = protocolMapper.getProtocolsByTCP();
            if (protocol == null) {
                throw new RuntimeException("协议配置不存在");
            }
            return protocol;
        } catch (Exception e) {
            throw new RuntimeException("获取协议配置失败: " + e.getMessage(), e);
        }
    }

    @Override
    public IotProtocol validateProtocolConfigIsNo(String protocolConfig) {
        try {
            IotProtocol protocol = protocolMapper.validateProtocolConfigIsNo(protocolConfig);
            if (protocol == null) {
                throw new RuntimeException("协议配置不存在");
            }
            return protocol;
        } catch (Exception e) {
            throw new RuntimeException("获取协议配置失败: " + e.getMessage(), e);
        }
    }

    @Override
    public List<IotProtocol> getProtocolsByIds(List<Long> protocolIdList) {
        return protocolMapper.getProtocolsByIds(protocolIdList);
    }
}