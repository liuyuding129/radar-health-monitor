package com.lz.radar.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.lz.radar.domain.dto.ProtocolConfigDto;
import lombok.Data;

import java.util.Date;

/**
 * 通信协议配置实体类
 * 对应表: iot_protocol
 */
@Data
@TableName(value = "iot_protocol")
public class IotProtocol {

    // 协议ID
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long protocolId;

    // 协议名称（MQTT/Modbus/CoAP/RTSP等）
    private String protocolName;

    // 协议说明
    private String description;

    // 协议配置内容（JSON格式）
    private String configJson;

    // 创建时间
    private Date createTime;

    // 备注
    private String remark;

    private String status;

    private String protocolType;

    // 发送Topic
    private Integer sendTopic;

    @TableField(exist = false)
    private ProtocolConfigDto protocolConfig;

    @TableField(exist = false)
    private Long deviceId;
}