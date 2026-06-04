package com.lz.radar.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IotTelemetryDTO {
    
    @JsonProperty("心率")
    private String heartRate;
    
    @JsonProperty("呼吸频率")
    private String respiratoryRate;
    
    @JsonProperty("环境气压")
    private String ambientPressure;
    
    @JsonProperty("环境湿度")
    private String ambientHumidity;
    
    @JsonProperty("环境温度")
    private String ambientTemperature;
    
    @JsonProperty("步频")
    private String stepFrequency;
    
    @JsonProperty("速度波动")
    private String speedFluctuation;
    
    @JsonProperty("声音强度")
    private String soundIntensity;

}