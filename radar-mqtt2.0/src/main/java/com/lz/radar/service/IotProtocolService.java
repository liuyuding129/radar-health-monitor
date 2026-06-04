package com.lz.radar.service;


import com.lz.radar.domain.IotProtocol;

import java.util.List;

/**
 * 通信协议配置服务接口
 * 定义协议相关的业务逻辑方法
 */
public interface IotProtocolService {
    
    /**
     * 获取所有协议配置
     * @return 协议配置列表
     */
    List<IotProtocol> getAllProtocols(String protocolType);
    
    /**
     * 根据协议ID获取协议配置
     * @param protocolId 协议ID
     * @return 协议配置信息
     */
    IotProtocol getProtocolById(Long protocolId);
    
    /**
     * 根据协议名称获取协议配置
     * @param protocolName 协议名称
     * @return 协议配置信息
     */
    IotProtocol getProtocolByName(String protocolName);
    
    /**
     * 添加新协议配置
     * @param protocol 协议配置信息
     * @return 是否添加成功
     */
    boolean addProtocol(IotProtocol protocol);
    
    /**
     * 更新协议配置
     * @param protocol 协议配置信息
     * @return 是否更新成功
     */
    boolean updateProtocol(IotProtocol protocol);
    
    /**
     * 删除协议配置
     * @param protocolId 协议ID
     * @return 是否删除成功
     */
    boolean deleteProtocol(Long protocolId);

    /**
     * 验证协议配置是否有效
     * @param protocol 协议配置
     * @return 是否有效
     */
    boolean validateProtocolConfig(IotProtocol protocol);

    /**
     * 根据协议类型获取协议配置列表
     * @return 协议配置列表
     */
    List<IotProtocol> getProtocolsByMQTT();

    /**
     * 根据协议类型获取协议配置列表
     * @return 协议配置列表
     */
    List<IotProtocol> getProtocolsByTCP();

    /**
     * 验证协议配置是否为空
     * @param protocol 协议配置
     * @return 是否为空
     */
    IotProtocol validateProtocolConfigIsNo(String protocolConfig);

    List<IotProtocol> getProtocolsByIds(List<Long> protocolIdList);
}