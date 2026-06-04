package com.lz.radar.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

/**
 * IoT设备信息实体类
 * 对应表: iot_device
 */
@Data
@TableName(value = "iot_device")
public class IotDevice {

    // 设备ID，自增主键
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long deviceId;

    // 设备名称
    private String deviceName;

    // 设备唯一编码
    private String deviceCode;

    // 设备类型，如传感器/网关/摄像头
    private String deviceType;

    // 状态（ONLINE/OFFLINE/FAULT），默认离线
    private String status;

    // 设备安装位置
    private String location;

    // 使用的通信协议ID
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private Long protocolId;

    // 创建时间
    private Date createTime;

    // 更新时间
    private Date updateTime;

    // 删除标志（0-正常，1-删除）
    private String delFlag;

    // 设备IP地址
    @TableField(exist = false)
    private String host;

    // 设备订阅号
    @TableField(exist = false)
    private String topic;

}