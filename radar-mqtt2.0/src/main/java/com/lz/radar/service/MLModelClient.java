package com.lz.radar.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * ML 模型 HTTP 客户端
 * 调用 Python Flask 微服务 (localhost:5001) 获取健康预测
 */
@Slf4j
@Service
public class MLModelClient {

    private static final String ML_SERVICE_URL = "http://localhost:5001";

    @Autowired
    private RestTemplate restTemplate;

    /**
     * 调用 ML 模型预测健康状态
     * @param heartRate 心率
     * @param respiratoryRate 呼吸频率
     * @param speedFluctuation 速度波动
     * @param stepFrequency 步频
     * @param vStd 速度标准差
     * @param currentDiff 频率差
     * @return 预测结果Map，包含 status, confidence, advice, summary 等
     */
    public Map<String, Object> predict(Number heartRate, Number respiratoryRate,
                                        Number speedFluctuation, Number stepFrequency,
                                        Number vStd, Number currentDiff) {
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("heart_rate", heartRate != null ? heartRate.doubleValue() : 75);
            request.put("respiratory_rate", respiratoryRate != null ? respiratoryRate.doubleValue() : 16);
            request.put("speed_fluctuation", speedFluctuation != null ? speedFluctuation.doubleValue() : 30);
            request.put("step_frequency", stepFrequency != null ? stepFrequency.doubleValue() : 0);
            request.put("v_std", vStd != null ? vStd.doubleValue() : 5);
            request.put("current_diff", currentDiff != null ? currentDiff.doubleValue() : 0);

            ResponseEntity<Map> response = restTemplate.postForEntity(
                    ML_SERVICE_URL + "/predict", request, Map.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.debug("ML 预测成功: {}", response.getBody());
                return response.getBody();
            }
        } catch (Exception e) {
            log.warn("ML 服务调用失败，将使用规则引擎: {}", e.getMessage());
        }
        return null;
    }

    /**
     * 检查 ML 服务是否可用
     */
    public boolean isAvailable() {
        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    ML_SERVICE_URL + "/health", Map.class);
            return response.getStatusCode().is2xxSuccessful()
                    && response.getBody() != null
                    && Boolean.TRUE.equals(response.getBody().get("model_loaded"));
        } catch (Exception e) {
            return false;
        }
    }
}
