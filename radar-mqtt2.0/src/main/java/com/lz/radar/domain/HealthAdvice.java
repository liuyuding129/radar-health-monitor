package com.lz.radar.domain;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 健康建议结果类
 * 由规则引擎生成的医学建议
 */
@Data
public class HealthAdvice {

    /** 心率状态: normal, elevated, low, dangerous */
    private String heartRateStatus;

    /** 呼吸状态: normal, elevated, low */
    private String respiratoryStatus;

    /** 压力水平: low, moderate, high */
    private String stressLevel;

    /** 综合健康评分 (0-100) */
    private Integer overallScore;

    /** 风险标识列表 */
    private List<String> riskFlags;

    /** 建议文本列表 */
    private List<String> adviceTexts;

    /** 生成时间 */
    private LocalDateTime generateTime;

    /**
     * 快速判断是否需要就医
     */
    public boolean needMedicalAttention() {
        return overallScore != null && overallScore < 60;
    }
}
