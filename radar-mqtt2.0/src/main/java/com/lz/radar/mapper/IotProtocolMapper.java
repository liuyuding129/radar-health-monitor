package com.lz.radar.mapper;

import com.lz.radar.domain.IotProtocol;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 通信协议配置数据访问层接口
 */
@Mapper
public interface IotProtocolMapper {
    
    /**
     * 查询所有协议配置
     * @return 协议配置列表
     */
    List<IotProtocol> selectAllProtocols(String protocolType);
    
    /**
     * 根据协议ID查询协议配置
     * @param protocolId 协议ID
     * @return 协议配置信息
     */
    IotProtocol selectProtocolById(@Param("protocolId") Long protocolId);
    
    /**
     * 根据协议名称查询协议配置
     * @param protocolName 协议名称
     * @return 协议配置信息
     */
    IotProtocol selectProtocolByName(@Param("protocolName") String protocolName);
    
    /**
     * 插入新协议配置
     * @param protocol 协议实体对象
     * @return 插入记录数
     */
    int insertProtocol(IotProtocol protocol);
    
    /**
     * 更新协议配置
     * @param protocol 协议实体对象
     * @return 更新记录数
     */
    int updateProtocol(IotProtocol protocol);
    
    /**
     * 根据协议ID删除协议配置
     * @param protocolId 协议ID
     * @return 删除记录数
     */
    int deleteProtocolById(@Param("protocolId") Long protocolId);

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
     * 验证协议配置是否已存在
     * @param protocolConfig 协议配置JSON
     * @return 是否已存在
     */
    IotProtocol validateProtocolConfigIsNo(String protocolConfig);

    List<IotProtocol> getProtocolsByIds(@Param("protocolIdList") List<Long> protocolIdList);
}