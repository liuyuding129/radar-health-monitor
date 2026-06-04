package com.lz.radar.utils;

import java.util.HashMap;
import java.util.Map;

public class DataUnitUtil {

    // ⚡ 单位映射表：key 为数据字段名（小写），value 为对应单位
    private static final Map<String, String> UNIT_MAP = new HashMap<>();

    static {
        // 环境类传感器
        UNIT_MAP.put("temperature", "℃");                // 温度
        UNIT_MAP.put("humidity", "%");                   // 湿度
        UNIT_MAP.put("illuminance", "lx");              // 光照度（LUX）
        UNIT_MAP.put("pressure", "hPa");                // 气压

        // 电量类传感器
        UNIT_MAP.put("voltage", "V");                    // 电压
        UNIT_MAP.put("current", "A");                    // 电流
        UNIT_MAP.put("power", "W");                      // 功率

        // 土壤类传感器
        UNIT_MAP.put("soiltemperature", "℃");           // 土壤温度
        UNIT_MAP.put("soilph", "pH");                    // 土壤PH值
        UNIT_MAP.put("soilmoisture", "%");              // 土壤水分
        UNIT_MAP.put("soilelectricalconductivity", "μS/cm"); // 土壤电导率

        // 气体类传感器
        UNIT_MAP.put("carbonmonoxide", "ppm");           // 一氧化碳
        UNIT_MAP.put("carbondioxide", "ppm");            // 二氧化碳

        // 光类传感器
        UNIT_MAP.put("ultravioletray", "mW/cm²");        // 紫外线
    }

    /**
     * 根据数据字段名获取对应单位
     *
     * @param dataKey 数据字段名，例如 "temperature"
     * @return 对应单位字符串，例如 "℃"，如果不存在返回 null
     */
    public static String getUnit(String dataKey) {
        if (dataKey == null) {
            return null; // 字段为空直接返回 null
        }
        // key 转小写，保证大小写不敏感
        return UNIT_MAP.get(dataKey.toLowerCase());
    }

}
