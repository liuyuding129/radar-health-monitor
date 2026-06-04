package com.lz.radar.domain.dto;

import lombok.Data;

@Data
public class ProtocolConfigDto {

    // 协议名称
    private String host;

    // 订阅号
    private String topic;

    // 端口
    private String port;
}
