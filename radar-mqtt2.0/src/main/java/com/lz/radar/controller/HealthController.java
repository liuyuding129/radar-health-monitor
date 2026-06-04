package com.lz.radar.controller;

import com.lz.radar.common.AjaxResult;
import com.lz.radar.configuration.MqttConnection;
import com.lz.radar.domain.HealthAdvice;
import com.lz.radar.service.HealthAdviceEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * 健康数据接口
 * 提供雷达健康监测数据和医学建议
 */
@RestController
@RequestMapping("/health")
public class HealthController {

    @Autowired
    private HealthAdviceEngine healthAdviceEngine;

    /**
     * 获取设备健康数据和医学建议
     * @param deviceId 设备ID
     * @return 包含雷达数据和健康建议的响应
     */
    @GetMapping("/data/{deviceId}")
    public AjaxResult getHealthData(@PathVariable Long deviceId) {
        if (deviceId == null || deviceId <= 0) {
            return AjaxResult.error("设备ID无效");
        }

        // 从MQTT连接缓存获取最新数据
        Map<Long, Map<String, Object>> allData = MqttConnection.getAllLatestData();
        Map<String, Object> radarData = allData.get(deviceId);

        // 生成健康建议
        HealthAdvice healthAdvice = healthAdviceEngine.generateAdvice(radarData);

        // 构建返回结果
        Map<String, Object> result = new HashMap<>();
        result.put("radarData", radarData != null ? radarData : new HashMap<>());
        result.put("healthAdvice", healthAdvice);
        result.put("lastUpdate", LocalDateTime.now().toString());

        return AjaxResult.success(result);
    }
}
