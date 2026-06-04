package com.lz.radar.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 设备原始通信消息实体类
 * 对应表: iot_message
 */
@Data
@TableName(value = "jy_management.iot_message")
public class IotMessage {

    // 消息ID
    private Long msgId;

    // 关联设备ID
    private Long deviceId;

    // 消息主题（如MQTT Topic）
    private String topic;

    // 原始报文内容
    private String payload;

    // 接收时间
    private Date recvTime;

    // 是否已解析，默认未解析
    private Boolean parsed;

}