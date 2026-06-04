package com.lz.radar.configuration.schedue;

import com.lz.radar.configuration.MqttManager;
import com.lz.radar.service.IotProtocolService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * 定时检查数据库配置是否变化
 * 若变化则重连对应 MQTT 客户端
 */
@Component
public class MqttRefreshTask {

    @Resource
    private IotProtocolService protocolConfigService;

    @Resource
    private MqttManager mqttManager;

    /**
     * 每30秒检查一次数据库
     */
    @Scheduled(fixedRate = 30000)
    public void refreshConnections() {
        System.out.println("🕒 检查 MQTT 配置变化...");
        mqttManager.initializeConnections(protocolConfigService.getProtocolsByMQTT());
    }
}
