package com.lz.radar.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

/**
 * 设备采集的时序数据实体类
 * 对应表: iot_telemetry
 */
@Data
@TableName(value = "jy_management.iot_telemetry")
public class IotTelemetry {

    // 数据记录ID
    private Long telemetryId;

    // 来源设备ID
    private Long deviceId;

    // 数据JSON
    private String dataJson;


    // 数据时间
    private Date recordTime;

    // 数据键名中文名称
    @TableField(exist = false)
    private String dataKeyChinese;
}