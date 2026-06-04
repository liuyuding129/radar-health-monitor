package com.lz.radar.domain;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("iot_protocol_type")
public class IotProtocolType {

    private String typeId;//编号

    private String typeName;//名称

    private Integer typeSort;//排序

    private String status;//状态

    private Date createTime;//创建时间

    private Date updateTime;//修改时间

}
