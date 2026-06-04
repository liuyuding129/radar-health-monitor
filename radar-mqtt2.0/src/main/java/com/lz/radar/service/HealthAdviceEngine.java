package com.lz.radar.service;

import com.lz.radar.domain.HealthAdvice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 健康建议引擎
 * 优先调用 ML 模型，规则引擎兜底
 */
@Slf4j
@Service
public class HealthAdviceEngine {

    // ========== 医学阈值常量 ==========

    /** 心率正常范围下限 */
    private static final int HR_NORMAL_LOW = 60;
    /** 心率正常范围上限 */
    private static final int HR_NORMAL_HIGH = 100;
    /** 运动时心率上限 */
    private static final int HR_EXERCISE_MAX = 150;
    /** 呼吸频率正常下限 */
    private static final int RR_NORMAL_LOW = 12;
    /** 呼吸频率正常上限 */
    private static final int RR_NORMAL_HIGH = 20;

    @Autowired(required = false)
    private MLModelClient mlModelClient;

    /**
     * 生成健康建议
     * @param dataMap 从MQTT获取的原始数据Map
     * @return 健康建议结果
     */
    public HealthAdvice generateAdvice(Map<String, Object> dataMap) {
        HealthAdvice advice = new HealthAdvice();
        List<String> riskFlags = new ArrayList<>();
        List<String> adviceTexts = new ArrayList<>();

        advice.setGenerateTime(LocalDateTime.now());

        if (dataMap == null || dataMap.isEmpty()) {
            advice.setOverallScore(0);
            advice.setHeartRateStatus("unknown");
            advice.setRespiratoryStatus("unknown");
            advice.setStressLevel("unknown");
            adviceTexts.add("暂无数据，请检查设备连接");
            advice.setAdviceTexts(adviceTexts);
            advice.setRiskFlags(riskFlags);
            return advice;
        }

        // 提取各字段值（10个参数）
        Integer heartRate = getIntegerValue(dataMap, "heartRate");
        Integer respiratoryRate = getIntegerValue(dataMap, "respiratoryRate");
        Double speedFluctuation = getDoubleValue(dataMap, "speedFluctuation");
        Double vStd = getDoubleValue(dataMap, "vStd");
        Double currentDiff = getDoubleValue(dataMap, "currentDiff");
        Double stepFrequency = getDoubleValue(dataMap, "stepFrequency");
        Double ambientTemperature = getDoubleValue(dataMap, "ambientTemperature");
        Double ambientHumidity = getDoubleValue(dataMap, "ambientHumidity");
        Double ambientPressure = getDoubleValue(dataMap, "ambientPressure");
        Double soundIntensity = getDoubleValue(dataMap, "soundIntensity");

        // 判断是否在运动（基于步频）
        boolean isMoving = (stepFrequency != null && stepFrequency > 0);

        // ========== 1. 规则引擎分析（始终执行）==========
        analyzeHeartRate(heartRate, isMoving, riskFlags, adviceTexts, advice);
        analyzeRespiration(respiratoryRate, riskFlags, adviceTexts, advice);

        // 步态分析只在运动时执行，否则提示静止状态
        if (isMoving) {
            analyzeFluctuation(speedFluctuation, riskFlags, adviceTexts, advice);
            analyzeGait(vStd, currentDiff, stepFrequency, riskFlags, adviceTexts, advice);
        } else {
            adviceTexts.add("【步态】未检测到步态数据，用户处于静止状态，步态分析暂不可用");
        }

        // 环境与声音分析
        analyzeEnvironment(ambientTemperature, ambientHumidity, ambientPressure, riskFlags, adviceTexts);
        analyzeSound(soundIntensity, riskFlags, adviceTexts);

        // ========== 2. 综合评分（规则引擎为主）==========
        // 静止时步态参数不参与评分，避免0值被当成"完美"
        int score;
        if (isMoving) {
            score = calculateOverallScore(heartRate, respiratoryRate, speedFluctuation,
                    vStd, currentDiff, stepFrequency, riskFlags);
        } else {
            // 静止状态只评心率和呼吸
            score = calculateOverallScore(heartRate, respiratoryRate, null, null, null, null, riskFlags);
        }

        // ========== 3. ML 模型辅助修正 ==========
        boolean mlSuccess = tryMLPrediction(heartRate, respiratoryRate, speedFluctuation,
                vStd, currentDiff, stepFrequency,
                advice, riskFlags, adviceTexts);

        // ML 只做小幅修正，不覆盖规则引擎评分
        if (mlSuccess && advice.getOverallScore() != null) {
            int mlScore = advice.getOverallScore();
            // ML 和规则各取权重：规则 70%，ML 30%
            score = (int) (score * 0.7 + mlScore * 0.3);
        }
        advice.setOverallScore(score);

        advice.setRiskFlags(riskFlags);
        advice.setAdviceTexts(adviceTexts);

        // 4. 生成总结性建议
        generateSummaryAdvice(advice, score, riskFlags);

        log.info("生成健康建议 - 评分: {}, 风险项: {}, ML: {}",
                score, riskFlags.size(), mlSuccess ? "成功" : "未使用");

        return advice;
    }

    /**
     * 尝试调用 ML 模型预测
     */
    private boolean tryMLPrediction(Integer heartRate, Integer respiratoryRate, Double speedFluctuation,
                                    Double vStd, Double currentDiff, Double stepFrequency,
                                    HealthAdvice advice, List<String> riskFlags, List<String> adviceTexts) {
        if (mlModelClient == null || !mlModelClient.isAvailable()) {
            log.debug("ML 服务不可用，使用规则引擎");
            return false;
        }

        try {
            Map<String, Object> prediction = mlModelClient.predict(
                    heartRate, respiratoryRate, speedFluctuation, stepFrequency, vStd, currentDiff
            );

            if (prediction == null) {
                return false;
            }

            String status = (String) prediction.get("status");
            String summary = (String) prediction.get("summary");
            String mlAdvice = (String) prediction.get("advice");
            Object confidence = prediction.get("confidence");

            log.info("ML 预测结果: {}, 置信度: {}", status, confidence);

            // 中医智能辨证建议
            if (summary != null && !summary.isEmpty()) {
                adviceTexts.add(0, "【中医智能辨证】" + summary);
            }
            if (mlAdvice != null && !mlAdvice.isEmpty()) {
                adviceTexts.add(1, "【调养建议】" + mlAdvice);
            }

            // 七情分析（基于 ML 情绪识别）
            String emotionAdvice = analyzeEmotion(status);
            if (emotionAdvice != null) {
                adviceTexts.add(2, "【七情分析】" + emotionAdvice);
            }

            // ML 评分
            int mlScore;
            if ("stress_high".equals(status)) mlScore = 55;
            else if ("mild_concern".equals(status)) mlScore = 70;
            else mlScore = 95;

            advice.setOverallScore(mlScore);

            return true;

        } catch (Exception e) {
            log.warn("ML 预测失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 七情分析：基于 ML 情绪状态给出中医七情（喜怒忧思悲恐惊）调摄建议
     */
    private String analyzeEmotion(String mlStatus) {
        if ("stress_high".equals(mlStatus)) {
            return "检测到情志不遂，肝气郁结。长期忧思恼怒可致肝郁化火，损伤心脾。" +
                    "建议：疏肝解郁，可练习深呼吸吐纳之法，配合按揉太冲穴、膻中穴。" +
                    "日常可饮用玫瑰花茶、陈皮水理气解郁，避免焦虑恼怒，保持心神安宁。";
        } else if ("mild_concern".equals(mlStatus)) {
            return "情绪略有波动，喜忧参半。适度的喜乐有助于气血流通，但喜太过则伤心。" +
                    "建议：保持心态平和，勿大喜大悲。可聆听舒缓音乐，练习冥想静坐，" +
                    "饮用百合莲子粥养心安神，使心神得养，情志调和。";
        } else if ("normal".equals(mlStatus)) {
            return "情志平和，心神安宁。七情调摄得当，有助于气血调和、脏腑安和。" +
                    "建议继续保持，适度运动如太极拳、八段锦，以调和阴阳、畅通经络。";
        }
        return null;
    }

    /**
     * 从Map中安全获取Integer值
     */
    private Integer getIntegerValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从Map中安全获取Number值
     */
    private Number getNumberValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return (Number) value;
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 从Map中安全获取Double值
     */
    private Double getDoubleValue(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 心率分析 - 中医脉象辨证
     */
    private void analyzeHeartRate(Integer heartRate, boolean isMoving,
                                  List<String> riskFlags, List<String> adviceTexts,
                                  HealthAdvice advice) {
        if (heartRate == null) {
            if (advice.getHeartRateStatus() == null) {
                advice.setHeartRateStatus("unknown");
            }
            return;
        }

        // 根据心率具体数值进行中医脉象辨证
        if (heartRate > 150) {
            advice.setHeartRateStatus("dangerous");
            riskFlags.add("dangerous_hr");
            adviceTexts.add("【脉象】脉疾数（心率" + heartRate + "次/分），一息七至以上。" +
                    "此为心阳亢盛或热入心包之危象，可见于外感热病极期或心阳暴脱之证。" +
                    "建议立即卧床休息，口服安宫牛黄丸或至宝丹开窍醒神，急送医院救治。");
        } else if (heartRate > 130) {
            advice.setHeartRateStatus("abnormal");
            riskFlags.add("abnormal_hr");
            adviceTexts.add("【脉象】脉数有力（心率" + heartRate + "次/分），一息六至。" +
                    "提示心火亢盛或痰热扰心，可见心悸怔忡、烦躁失眠、口舌生疮等症。" +
                    "治法：清心泻火，安神定志。方药：朱砂安神丸或黄连温胆汤加减。" +
                    "食疗：莲子心茶、百合地黄粥。针刺：神门、内关、三阴交，泻法。");
        } else if (heartRate > 110) {
            advice.setHeartRateStatus("elevated");
            riskFlags.add("tachycardia");
            adviceTexts.add("【脉象】脉数（心率" + heartRate + "次/分），一息五至余。" +
                    "多为阴虚火旺或肝郁化火所致，伴见心悸、口干咽燥、手足心热。" +
                    "治法：滋阴降火，养心安神。方药：天王补心丹或知柏地黄丸加减。" +
                    "日常调护：避免熬夜，可服枸杞菊花茶滋阴清热。穴位按摩：太溪、神门。");
        } else if (heartRate > 100) {
            advice.setHeartRateStatus("slightly_elevated");
            riskFlags.add("high_hr");
            adviceTexts.add("【脉象】脉略数（心率" + heartRate + "次/分）。" +
                    "轻度心火偏旺或情绪紧张所致，可见于情志不遂、心神不宁。" +
                    "建议：静心调息，练习腹式呼吸。可饮用菊花决明子茶清肝明目，" +
                    "配合按揉内关穴、膻中穴宽胸理气。避免浓茶咖啡。");
        } else if (heartRate >= 60 && heartRate <= 100) {
            advice.setHeartRateStatus("normal");
            adviceTexts.add("【脉象】脉象和缓有力（心率" + heartRate + "次/分），一息四至五至。" +
                    "此为平人之脉，胃气充盈，气血调和。心主血脉功能正常，神明得养。" +
                    "日常养护：保持规律作息，子时入眠以养心血。可练习八段锦之" +
                    "\"摇头摆尾去心火\"，早晚各一组，以养心安神。");
        } else if (heartRate >= 50 && heartRate < 60) {
            advice.setHeartRateStatus("low");
            riskFlags.add("bradycardia");
            adviceTexts.add("【脉象】脉迟（心率" + heartRate + "次/分），一息三至余。" +
                    "多为阳气不足，心阳不振，鼓动无力。可见神疲乏力、畏寒肢冷、面色白。" +
                    "治法：温补心阳，益气复脉。方药：桂枝甘草汤合保元汤加减。" +
                    "食疗：当归羊肉汤温补气血。艾灸：心俞、膻中、神阙温阳益气。");
        } else if (heartRate >= 40 && heartRate < 50) {
            advice.setHeartRateStatus("abnormal");
            riskFlags.add("abnormal_hr");
            adviceTexts.add("【脉象】脉迟无力（心率" + heartRate + "次/分）。" +
                    "提示心肾阳虚，命门火衰，可见腰膝酸冷、小便清长、下肢浮肿。" +
                    "治法：温补心肾，益火消阴。方药：金匮肾气丸合桂枝甘草龙骨牡蛎汤。" +
                    "建议：避免寒凉饮食，注意保暖。可艾灸关元、命门温补肾阳。");
        } else if (heartRate < 40) {
            advice.setHeartRateStatus("dangerous");
            riskFlags.add("dangerous_hr");
            adviceTexts.add("【脉象】脉微欲绝（心率" + heartRate + "次/分），一息不足二至。" +
                    "此为阳气衰微、心阳欲脱之危候，可见四肢厥冷、冷汗淋漓、神志淡漠。" +
                    "治法：回阳救逆，益气固脱。方药：参附汤或四逆汤急煎服用。" +
                    "立即就医抢救，必要时安装心脏起搏器。");
        }
    }

    /**
     * 呼吸分析 - 中医肺气辨证
     */
    private void analyzeRespiration(Integer respiratoryRate,
                                    List<String> riskFlags, List<String> adviceTexts,
                                    HealthAdvice advice) {
        if (respiratoryRate == null) {
            if (advice.getRespiratoryStatus() == null) {
                advice.setRespiratoryStatus("unknown");
            }
            return;
        }

        if (respiratoryRate > 30) {
            advice.setRespiratoryStatus("dangerous");
            riskFlags.add("dangerous_rr");
            adviceTexts.add("【呼吸】呼吸急促（" + respiratoryRate + "次/分），肺气大虚，肾不纳气。" +
                    "可见喘促气短、张口抬肩、汗出肢冷，为肺肾两虚之危候。" +
                    "治法：补肺纳肾，降气平喘。方药：参蚧散或都气丸加减。" +
                    "紧急处理：半卧位休息，给予吸氧。针刺定喘、肺俞、肾俞。");
        } else if (respiratoryRate > 25) {
            advice.setRespiratoryStatus("elevated");
            riskFlags.add("tachypnea");
            adviceTexts.add("【呼吸】呼吸偏快（" + respiratoryRate + "次/分），肺气上逆，宣降失常。" +
                    "可见胸闷气短、咳嗽痰黄，为痰热壅肺或肝火犯肺之象。" +
                    "治法：宣肺清热，化痰止咳。方药：麻杏石甘汤或泻白散加减。" +
                    "食疗：川贝雪梨膏润肺化痰。穴位按摩：列缺、尺泽、丰隆化痰平喘。");
        } else if (respiratoryRate > 20) {
            advice.setRespiratoryStatus("slightly_elevated");
            riskFlags.add("high_rr");
            adviceTexts.add("【呼吸】呼吸略促（" + respiratoryRate + "次/分），肺气稍有不利。" +
                    "可能因情志紧张或外感初起所致，气机略有不畅。" +
                    "建议：调息静气，练习六字诀中\"嘘\"字诀以疏肝理气、\"呬\"字诀以润肺。" +
                    "可饮用薄荷茶轻宣肺气。注意室内通风，避免闷热环境。");
        } else if (respiratoryRate >= 12 && respiratoryRate <= 20) {
            advice.setRespiratoryStatus("normal");
            adviceTexts.add("【呼吸】呼吸均匀平和（" + respiratoryRate + "次/分），肺气宣降正常。" +
                    "肺主气，司呼吸，肺气和则呼吸调匀。说明肺气充盈，卫气固密，营卫调和。" +
                    "日常调养：练习八段锦中\"左右开弓似射雕\"以扩胸理肺，\"调理脾胃须单举\"以培土生金。" +
                    "秋季可多食百合、银耳、梨等润肺之品，以养肺阴。");
        } else if (respiratoryRate >= 8 && respiratoryRate < 12) {
            advice.setRespiratoryStatus("low");
            riskFlags.add("bradypnea");
            adviceTexts.add("【呼吸】呼吸偏缓（" + respiratoryRate + "次/分），肺气不足，宗气虚弱。" +
                    "可见少气懒言、语声低微、体倦乏力。脾为气血生化之源，培土生金为治本之法。" +
                    "治法：补益肺脾，培土生金。方药：补中益气汤合四君子汤加减。" +
                    "食疗：黄芪炖乌鸡补益气血。艾灸肺俞、脾俞、足三里。");
        } else if (respiratoryRate < 8) {
            advice.setRespiratoryStatus("dangerous");
            riskFlags.add("dangerous_rr");
            adviceTexts.add("【呼吸】呼吸微弱（" + respiratoryRate + "次/分），肺气将绝。" +
                    "提示元气大虚，五脏之气将脱，可见呼吸浅促、面色苍白、冷汗淋漓。" +
                    "治法：大补元气，固脱救逆。方药：独参汤急煎频服。" +
                    "紧急就医处理。");
        }
    }

    /**
     * 平均速度分析（speedFluctuation实际是v_avg平均速度，单位cm/s）
     * 正常范围: 90-140 cm/s
     */
    private void analyzeFluctuation(Double speedFluctuation,
                                    List<String> riskFlags, List<String> adviceTexts,
                                    HealthAdvice advice) {
        if (speedFluctuation == null) {
            if (advice.getStressLevel() == null) {
                advice.setStressLevel("unknown");
            }
            return;
        }

        // 平均速度正常范围 90-140 cm/s
        if (speedFluctuation < 90) {
            advice.setStressLevel("moderate");
            riskFlags.add("low_velocity");
            adviceTexts.add("【平均速度】行走速度偏慢（" + speedFluctuation.intValue() + "cm/s）。" +
                    "步履迟缓多为肝肾不足、气血两虚，筋骨失于濡养。" +
                    "治法：补益肝肾，强筋壮骨。方药：独活寄生汤或虎潜丸加减。");
        } else if (speedFluctuation > 140) {
            advice.setStressLevel("high");
            riskFlags.add("high_velocity");
            adviceTexts.add("【平均速度】行走速度偏快（" + speedFluctuation.intValue() + "cm/s）。" +
                    "过速行走耗伤气血，久则肝肾亏虚。建议放缓步伐，匀速缓行为佳。");
        } else {
            advice.setStressLevel("low");
            adviceTexts.add("【平均速度】行走速度适中（" + speedFluctuation.intValue() + "cm/s），步伐稳健。");
        }
    }

    /**
     * 步态分析（速度标准差vStd单位cm/s，频率差currentDiff单位Hz，步频stepFrequency单位Hz）
     */
    private void analyzeGait(Double vStd, Double currentDiff, Double stepFrequency,
                             List<String> riskFlags, List<String> adviceTexts,
                             HealthAdvice advice) {
        // 步频分析（单位Hz，正常1.5-2.0）
        if (stepFrequency != null) {
            if (stepFrequency > 20) {
                // 步频>20 Hz 说明传过来的可能是步/分，需要转换（兼容旧数据）
                double freqHz = stepFrequency / 60.0;
                analyzeStepFreqHz(freqHz, riskFlags, adviceTexts);
            } else if (stepFrequency > 0) {
                analyzeStepFreqHz((double) stepFrequency, riskFlags, adviceTexts);
            }
        }

        // 速度标准差分析（单位cm/s，<15正常，15-25轻度异常，>25明显异常）
        if (vStd != null) {
            if (vStd > 25) {
                riskFlags.add("unstable_gait");
                adviceTexts.add("【步态稳定性】步态明显不稳，速度波动大（vStd=" + String.format("%.1f", vStd) + "cm/s）。" +
                        "提示气血两虚或肝风内动。肝主筋，筋失濡养则运动不协调。" +
                        "治法：养血柔肝，息风止痉。方药：芍药甘草汤合镇肝熄风汤加减。" +
                        "建议：加强平衡训练，避免单独行走于不平坦路面。");
            } else if (vStd > 15) {
                riskFlags.add("mild_unstable_gait");
                adviceTexts.add("【步态稳定性】步态轻度不稳（vStd=" + String.format("%.1f", vStd) + "cm/s）。" +
                        "提示气血稍亏，筋骨濡养不足。建议适度锻炼，养血柔肝。" +
                        "可练习八段锦\"摇头摆尾去心火\"、太极拳云手，培养沉稳步态。");
            }
        }

        // 频率差分析（单位Hz，<0.03正常，~0.06轻度异常，>0.08明显异常）
        if (currentDiff != null) {
            double absDiff = Math.abs(currentDiff);
            if (absDiff > 0.08) {
                riskFlags.add("irregular_rhythm");
                adviceTexts.add("【步态节奏】行走节奏明显不规律（频率差=" + String.format("%.3f", currentDiff) + "Hz）。" +
                        "提示经脉气血运行不畅，肝疏泄功能失调，气机紊乱。" +
                        "建议：练习匀速步行，配合呼吸吐纳，意守丹田，以调畅气机。" +
                        "可配合柴胡疏肝散调理气机，恢复气血运行节律。");
            } else if (absDiff > 0.06) {
                riskFlags.add("mild_irregular_rhythm");
                adviceTexts.add("【步态节奏】行走节奏轻度不规律（频率差=" + String.format("%.3f", currentDiff) + "Hz）。" +
                        "气机略有不畅，建议练习匀速步行，调息宁神。");
            }
        }
    }

    /**
     * 步频分析（Hz单位，正常1.5-2.0）
     */
    private void analyzeStepFreqHz(double freqHz, List<String> riskFlags, List<String> adviceTexts) {
        if (freqHz > 2.0) {
            riskFlags.add("high_step_freq");
            adviceTexts.add("【步态】步频过高（" + String.format("%.2f", freqHz) + "Hz），行走急促。" +
                    "过速行走耗伤气血，久则肝肾亏虚。建议放缓步伐，以匀速缓行为佳。" +
                    "可配合太极拳中\"云手\"动作练习，培养沉稳步态，调和气血。");
        } else if (freqHz < 1.5) {
            riskFlags.add("low_step_freq");
            adviceTexts.add("【步态】步频偏低（" + String.format("%.2f", freqHz) + "Hz），步态迟缓。" +
                    "中医认为步履蹒跚多为肝肾不足、气血两虚，筋骨失于濡养。" +
                    "治法：补益肝肾，强筋壮骨。方药：独活寄生汤或虎潜丸加减。" +
                    "食疗：杜仲牛膝煲猪腰、黑芝麻核桃粥。练习八段锦\"两手攀足固肾腰\"强腰健肾。");
        } else {
            adviceTexts.add("【步态】步频适中（" + String.format("%.2f", freqHz) + "Hz），步伐稳健。" +
                    "肝主筋，肾主骨，步态稳健说明肝肾充盈，筋骨得养。");
        }
    }

    /**
     * 综合评分 (0-100) - 渐进式扣分
     * 新阈值体系：v_avg(cm/s):90-140, step_freq(Hz):1.5-2.0, v_std(cm/s):<15正常15-25轻度>25异常, freq_diff(Hz):<0.03正常0.06轻度>0.08异常
     */
    private int calculateOverallScore(Integer heartRate, Integer respiratoryRate,
                                      Double speedFluctuation, Double vStd, Double currentDiff,
                                      Double stepFrequency, List<String> riskFlags) {
        int score = 100;

        // 心率扣分（权重最大）
        if (heartRate != null) {
            if (heartRate >= 60 && heartRate <= 100) {
                // 正常区间，不扣分
            } else if (heartRate > 100 && heartRate <= 120) {
                score -= (heartRate - 100) * 2;       // 100→120 扣 0~40
            } else if (heartRate > 120 && heartRate <= 150) {
                score -= 40 + (heartRate - 120) * 1;   // 120→150 扣 40~70
            } else if (heartRate > 150) {
                score -= 70 + Math.min(20, (heartRate - 150));  // >150 扣 70~90
            } else if (heartRate < 60 && heartRate >= 50) {
                score -= (60 - heartRate) * 2;          // 50→60 扣 0~20
            } else if (heartRate < 50 && heartRate >= 40) {
                score -= 20 + (50 - heartRate) * 2;     // 40→50 扣 20~40
            } else if (heartRate < 40) {
                score -= 40 + Math.min(40, (40 - heartRate) * 3); // <40 扣 40~80
            }
        }

        // 呼吸频率扣分
        if (respiratoryRate != null) {
            if (respiratoryRate >= 12 && respiratoryRate <= 20) {
                // 正常
            } else if (respiratoryRate > 20 && respiratoryRate <= 25) {
                score -= (respiratoryRate - 20) * 4;     // 20→25 扣 0~20
            } else if (respiratoryRate > 25 && respiratoryRate <= 30) {
                score -= 20 + (respiratoryRate - 25) * 4; // 25→30 扣 20~40
            } else if (respiratoryRate > 30) {
                score -= 40 + Math.min(30, (respiratoryRate - 30) * 4); // >30 扣 40~70
            } else if (respiratoryRate < 12 && respiratoryRate >= 8) {
                score -= (12 - respiratoryRate) * 3;     // 8→12 扣 0~12
            } else if (respiratoryRate < 8) {
                score -= 12 + Math.min(30, (8 - respiratoryRate) * 5); // <8 扣 12~42
            }
        }

        // 平均速度扣分（speedFluctuation实际是v_avg，单位cm/s，正常90-140）
        if (speedFluctuation != null) {
            if (speedFluctuation >= 90 && speedFluctuation <= 140) {
                // 正常区间，不扣分
            } else if (speedFluctuation < 90 && speedFluctuation >= 50) {
                score -= (int) ((90 - speedFluctuation) * 0.3);  // 50→90 扣 0~12
            } else if (speedFluctuation < 50) {
                score -= 12 + Math.min(15, (int) ((50 - speedFluctuation) * 0.5)); // <50 扣 12~27
            } else if (speedFluctuation > 140 && speedFluctuation <= 180) {
                score -= (int) ((speedFluctuation - 140) * 0.3);  // 140→180 扣 0~12
            } else if (speedFluctuation > 180) {
                score -= 12 + Math.min(15, (int) ((speedFluctuation - 180) * 0.3)); // >180 扣 12~27
            }
        }

        // 速度标准差扣分（vStd单位cm/s，<15正常，15-25扣分，>25多扣）
        if (vStd != null) {
            if (vStd <= 15) {
                // 正常，不扣分
            } else if (vStd <= 25) {
                score -= (int) ((vStd - 15) * 1.0);  // 15→25 扣 0~10
            } else {
                score -= 10 + Math.min(15, (int) ((vStd - 25) * 1.0)); // >25 扣 10~25
            }
        }

        // 频率差扣分（currentDiff单位Hz，<0.03正常，0.06轻度，>0.08异常）
        if (currentDiff != null) {
            double absDiff = Math.abs(currentDiff);
            if (absDiff <= 0.03) {
                // 正常，不扣分
            } else if (absDiff <= 0.06) {
                score -= (int) ((absDiff - 0.03) * 100);  // 0.03→0.06 扣 0~3
            } else if (absDiff <= 0.08) {
                score -= 3 + (int) ((absDiff - 0.06) * 100);  // 0.06→0.08 扣 3~5
            } else {
                score -= 5 + Math.min(10, (int) ((absDiff - 0.08) * 100));  // >0.08 扣 5~15
            }
        }

        // 风险标记额外扣分
        score -= riskFlags.size() * 3;

        return Math.max(0, Math.min(100, score));
    }

    /**
     * 生成总结性建议
     */
    private void generateSummaryAdvice(HealthAdvice advice, int score, List<String> riskFlags) {
        List<String> adviceTexts = advice.getAdviceTexts();

        if (score >= 80) {
            adviceTexts.add("综合评估：气血调和，脏腑功能正常，望闻问切四诊合参，患者状态良好");
        } else if (score >= 60) {
            adviceTexts.add("综合评估：气血略有失调，建议辨证施治，调理脏腑功能，定期复测观察");
        } else if (score >= 40) {
            adviceTexts.add("综合评估：阴阳失衡明显，建议中医辨证论治，配合针灸推拿等综合调理");
        } else {
            adviceTexts.add("综合评估：正气亏虚，邪气偏盛，建议紧急中医处置，必要时中西医结合治疗");
        }
    }

    /**
     * 环境分析 - 中医六淫辨证（风、寒、暑、湿、燥、火）
     * 根据温度、湿度、气压判断当前环境易发的中医病邪
     */
    private void analyzeEnvironment(Double temperature, Double humidity, Double pressure,
                                     List<String> riskFlags, List<String> adviceTexts) {
        if (temperature == null && humidity == null && pressure == null) return;

        StringBuilder envAdvice = new StringBuilder("【环境六淫分析】");
        boolean hasWarning = false;

        // 温度分析
        if (temperature != null) {
            if (temperature > 35) {
                envAdvice.append("当前气温").append(temperature.intValue()).append("°C，属暑热之邪。");
                envAdvice.append("暑易伤津耗气，可见口渴多汗、乏力倦怠，重则中暑。");
                envAdvice.append("建议：避暑降温，多饮绿豆汤、西瓜汁清热解暑。可服藿香正气水防暑。");
                riskFlags.add("high_temp");
                hasWarning = true;
            } else if (temperature > 30) {
                envAdvice.append("气温偏高（").append(temperature.intValue()).append("°C），热邪偏盛。");
                envAdvice.append("易致心火上炎，可见心烦口渴、小便短赤。");
                envAdvice.append("建议：适当降温，饮用菊花茶、薄荷茶清心降火。");
                riskFlags.add("warm_temp");
                hasWarning = true;
            } else if (temperature < 5) {
                envAdvice.append("当前气温").append(temperature.intValue()).append("°C，寒邪偏盛。");
                envAdvice.append("寒为阴邪，易伤阳气，可见恶寒发热、关节疼痛、腹痛泄泻。");
                envAdvice.append("建议：注意保暖防寒，可饮用生姜红糖汤温中散寒。艾灸关元、气海温补阳气。");
                riskFlags.add("cold_temp");
                hasWarning = true;
            } else if (temperature < 10) {
                envAdvice.append("气温偏低（").append(temperature.intValue()).append("°C），有寒邪侵袭之虞。");
                envAdvice.append("易发风寒感冒、关节酸痛。");
                envAdvice.append("建议：注意添衣保暖，可服桂枝汤调和营卫。");
                riskFlags.add("cool_temp");
                hasWarning = true;
            }
        }

        // 湿度分析
        if (humidity != null) {
            if (humidity > 80) {
                envAdvice.append(" 湿度").append(humidity.intValue()).append("%，湿邪偏重。");
                envAdvice.append("湿为阴邪，易阻气机，损伤脾阳。可见头身困重、脘腹痞闷、食欲不振、大便黏滞。");
                envAdvice.append("建议：健脾祛湿，可服参苓白术散。食疗：薏仁红豆粥、冬瓜汤。避免居住潮湿环境。");
                riskFlags.add("high_humidity");
                hasWarning = true;
            } else if (humidity > 70) {
                envAdvice.append(" 湿度偏高（").append(humidity.intValue()).append("%），有湿邪之患。");
                envAdvice.append("脾恶湿，湿盛则影响运化功能。");
                envAdvice.append("建议：注意室内通风除湿，可饮陈皮茶理气化湿。");
                riskFlags.add("mild_humidity");
                hasWarning = true;
            } else if (humidity < 30) {
                envAdvice.append(" 湿度").append(humidity.intValue()).append("%，气候干燥，燥邪当令。");
                envAdvice.append("燥易伤肺，可见干咳少痰、咽干鼻燥、皮肤干裂。");
                envAdvice.append("建议：润燥养肺，可服沙参麦冬汤。食疗：百合银耳羹、梨汁润肺。室内可使用加湿器。");
                riskFlags.add("dry_humidity");
                hasWarning = true;
            }
        }

        // 气压分析
        if (pressure != null) {
            if (pressure < 1000) {
                envAdvice.append(" 气压偏低（").append(pressure.intValue()).append("hPa），天人相应，低气压易致气机郁滞。");
                envAdvice.append("可加重胸闷气短、关节酸痛等宿疾，风湿痹痛者尤需注意。");
                envAdvice.append("建议：减少户外活动，关节疼痛者可贴敷活血膏药，艾灸阿是穴温经通络。");
                riskFlags.add("low_pressure");
                hasWarning = true;
            }
        }

        // 综合判断：湿热、寒湿、燥热等复合病邪
        if (temperature != null && humidity != null) {
            if (temperature > 28 && humidity > 70) {
                envAdvice.append(" 【注意】高温高湿环境，湿热之邪合而为患。");
                envAdvice.append("易发湿疹、痱子、脾胃湿热（恶心、腹泻）等。");
                envAdvice.append("建议：清热利湿，可服龙胆泻肝汤或三仁汤加减。忌食辛辣油腻。");
                riskFlags.add("damp_heat");
            } else if (temperature < 10 && humidity > 70) {
                envAdvice.append(" 【注意】低温高湿环境，寒湿之邪偏盛。");
                envAdvice.append("易发关节疼痛（寒湿痹证）、脘腹冷痛。");
                envAdvice.append("建议：温中散寒祛湿，可服附子理中丸。艾灸中脘、足三里。");
                riskFlags.add("cold_damp");
            } else if (temperature > 28 && humidity < 40) {
                envAdvice.append(" 【注意】高温干燥环境，温燥之邪伤肺。");
                envAdvice.append("易发干咳、咽痛、鼻出血。");
                envAdvice.append("建议：清燥救肺，可服清燥救肺汤。多饮蜂蜜水、梨汁。");
                riskFlags.add("warm_dry");
            }
        }

        if (!hasWarning && temperature != null && humidity != null) {
            envAdvice.append("当前温湿度适宜，风气调和，不易感受外邪。");
            envAdvice.append("建议保持良好生活习惯，顺应四时变化。");
        }

        adviceTexts.add(envAdvice.toString());
    }

    /**
     * 声音分析 - 中医闻诊
     * 根据声音强度判断情志与气机状态
     */
    private void analyzeSound(Double soundIntensity, List<String> riskFlags, List<String> adviceTexts) {
        if (soundIntensity == null) return;

        if (soundIntensity > 80) {
            adviceTexts.add("【闻诊】声息高亢（音量" + soundIntensity.intValue() + "dB）。" +
                    "中医闻诊认为，语声高亢有力多为实证、热证，可见于肝阳上亢或心火亢盛之人。" +
                    "若伴有急躁易怒，提示肝气郁结化火。建议：平肝潜阳，可服天麻钩藤饮。" +
                    "日常可练习静坐冥想，聆听古琴、流水等舒缓之音以宁心安神。");
            riskFlags.add("loud_sound");
        } else if (soundIntensity > 60) {
            adviceTexts.add("【闻诊】语声偏响（音量" + soundIntensity.intValue() + "dB）。" +
                    "声气较盛，提示正气尚充，但需注意情志调摄，避免恼怒伤肝。");
        } else if (soundIntensity >= 30 && soundIntensity <= 60) {
            adviceTexts.add("【闻诊】语声适中（音量" + soundIntensity.intValue() + "dB）。" +
                    "中医认为声息均匀适中为正气充沛之象，肺气宣降正常，中气十足。");
        } else if (soundIntensity < 30 && soundIntensity > 10) {
            adviceTexts.add("【闻诊】语声低微（音量" + soundIntensity.intValue() + "dB）。" +
                    "声低气怯多为虚证，可见于肺气不足或心气虚弱。" +
                    "常见少气懒言、语声低微、倦怠乏力等症。" +
                    "治法：补益肺脾之气。方药：补中益气汤合玉屏风散加减。" +
                    "食疗：黄芪炖鸡、人参大枣粥。平时可练习六字诀中\"呬\"字诀补肺气。");
            riskFlags.add("low_sound");
        } else if (soundIntensity <= 10) {
            adviceTexts.add("【闻诊】环境安静或声息极微（音量" + soundIntensity.intValue() + "dB）。" +
                    "若处于静息状态属正常；若持续无声息，需关注是否为极度疲惫或气虚欲脱之象。");
        }
    }
}
