package com.lz.radar.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 10进制数据解析工具类
 * 用于解析MQTT接收的雷达数据
 * 
 * @author lz
 * @date 2026-04-03
 */
@Slf4j
@Component
public class DecimalDataParser {

    /**
     * 字节序枚举
     */
    public enum ByteOrderType {
        BIG_ENDIAN,    // 大端序（网络字节序）
        LITTLE_ENDIAN  // 小端序
    }

    /**
     * 解析结果封装类
     */
    public static class ParseResult {
        private int decimalValue;      // 十进制值
        private String hexValue;       // 十六进制值
        private String binaryValue;    // 二进制值
        private String asciiValue;     // ASCII字符（如果可读）
        private List<Integer> byteList; // 字节列表

        // Getters and Setters
        public int getDecimalValue() { return decimalValue; }
        public void setDecimalValue(int decimalValue) { this.decimalValue = decimalValue; }
        public String getHexValue() { return hexValue; }
        public void setHexValue(String hexValue) { this.hexValue = hexValue; }
        public String getBinaryValue() { return binaryValue; }
        public void setBinaryValue(String binaryValue) { this.binaryValue = binaryValue; }
        public String getAsciiValue() { return asciiValue; }
        public void setAsciiValue(String asciiValue) { this.asciiValue = asciiValue; }
        public List<Integer> getByteList() { return byteList; }
        public void setByteList(List<Integer> byteList) { this.byteList = byteList; }

        @Override
        public String toString() {
            return String.format("ParseResult{decimal=%d, hex=%s, binary=%s, ascii='%s'}",
                    decimalValue, hexValue, binaryValue, asciiValue);
        }
    }

    /**
     * 雷达数据解析结果
     */
    public static class RadarData {
        private int distance;          // 距离（厘米或毫米）
        private int speed;             // 速度（km/h）
        private int angle;             // 角度（度）
        private int signalStrength;    // 信号强度
        private long timestamp;        // 时间戳
        private Map<String, Object> extraData; // 额外数据

        public RadarData() {
            this.extraData = new HashMap<>();
            this.timestamp = System.currentTimeMillis();
        }

        // Getters and Setters
        public int getDistance() { return distance; }
        public void setDistance(int distance) { this.distance = distance; }
        public int getSpeed() { return speed; }
        public void setSpeed(int speed) { this.speed = speed; }
        public int getAngle() { return angle; }
        public void setAngle(int angle) { this.angle = angle; }
        public int getSignalStrength() { return signalStrength; }
        public void setSignalStrength(int signalStrength) { this.signalStrength = signalStrength; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
        public Map<String, Object> getExtraData() { return extraData; }
        public void setExtraData(Map<String, Object> extraData) { this.extraData = extraData; }

        @Override
        public String toString() {
            return String.format("RadarData{distance=%d%s, speed=%dkm/h, angle=%d°, strength=%d, timestamp=%d}",
                    distance, distance > 100 ? "cm" : "m", speed, angle, signalStrength, timestamp);
        }
    }

    /**
     * 1. 单个字节转10进制（0-255）
     */
    public int byteToDecimal(byte b) {
        return b & 0xFF;
    }

    /**
     * 2. 字节数组转10进制列表
     */
    public List<Integer> bytesToDecimalList(byte[] bytes) {
        List<Integer> decimalList = new ArrayList<>();
        for (byte b : bytes) {
            decimalList.add(b & 0xFF);
        }
        return decimalList;
    }

    /**
     * 3. 2个字节转10进制（大端序）
     */
    public int twoBytesToDecimalBigEndian(byte high, byte low) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    /**
     * 4. 2个字节转10进制（小端序）
     */
    public int twoBytesToDecimalLittleEndian(byte low, byte high) {
        return ((high & 0xFF) << 8) | (low & 0xFF);
    }

    /**
     * 5. 4个字节转10进制（大端序）
     */
    public int fourBytesToDecimalBigEndian(byte b1, byte b2, byte b3, byte b4) {
        return ((b1 & 0xFF) << 24) |
               ((b2 & 0xFF) << 16) |
               ((b3 & 0xFF) << 8)  |
               (b4 & 0xFF);
    }

    /**
     * 6. 4个字节转10进制（小端序）
     */
    public int fourBytesToDecimalLittleEndian(byte b1, byte b2, byte b3, byte b4) {
        return ((b4 & 0xFF) << 24) |
               ((b3 & 0xFF) << 16) |
               ((b2 & 0xFF) << 8)  |
               (b1 & 0xFF);
    }

    /**
     * 7. 字节数组转整型（指定字节序）
     */
    public int bytesToInt(byte[] bytes, ByteOrderType order) {
        if (bytes == null || bytes.length > 4) {
            throw new IllegalArgumentException("字节数组长度必须在1-4之间");
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(4);
        if (order == ByteOrderType.BIG_ENDIAN) {
            buffer.order(java.nio.ByteOrder.BIG_ENDIAN);
        } else {
            buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        }
        
        // 补齐到4字节
        byte[] temp = new byte[4];
        if (order == ByteOrderType.BIG_ENDIAN) {
            System.arraycopy(bytes, 0, temp, 4 - bytes.length, bytes.length);
        } else {
            System.arraycopy(bytes, 0, temp, 0, bytes.length);
        }
        
        buffer.put(temp);
        buffer.flip();
        return buffer.getInt();
    }

    /**
     * 8. 字节数组转长整型（8字节，支持大数值）
     */
    public long bytesToLong(byte[] bytes, ByteOrderType order) {
        if (bytes == null || bytes.length > 8) {
            throw new IllegalArgumentException("字节数组长度必须在1-8之间");
        }
        
        ByteBuffer buffer = ByteBuffer.allocate(8);
        if (order == ByteOrderType.BIG_ENDIAN) {
            buffer.order(java.nio.ByteOrder.BIG_ENDIAN);
        } else {
            buffer.order(java.nio.ByteOrder.LITTLE_ENDIAN);
        }
        
        byte[] temp = new byte[8];
        if (order == ByteOrderType.BIG_ENDIAN) {
            System.arraycopy(bytes, 0, temp, 8 - bytes.length, bytes.length);
        } else {
            System.arraycopy(bytes, 0, temp, 0, bytes.length);
        }
        
        buffer.put(temp);
        buffer.flip();
        return buffer.getLong();
    }

    /**
     * 9. BCD码转10进制（常用于时间、日期）
     */
    public int bcdToDecimal(byte bcd) {
        int high = (bcd >> 4) & 0x0F;
        int low = bcd & 0x0F;
        return high * 10 + low;
    }

    /**
     * 10. 2字节BCD码转10进制
     */
    public int twoBytesBcdToDecimal(byte high, byte low) {
        return bcdToDecimal(high) * 100 + bcdToDecimal(low);
    }

    /**
     * 11. 有符号字节转10进制（-128到127）
     */
    public int signedByteToDecimal(byte b) {
        return (int) b;
    }

    /**
     * 12. 无符号字节转10进制（0-255）
     */
    public int unsignedByteToDecimal(byte b) {
        return b & 0xFF;
    }

    /**
     * 13. 解析字节数组，返回详细信息
     */
    public ParseResult parseDetailed(byte b) {
        ParseResult result = new ParseResult();
        int decimal = b & 0xFF;
        result.setDecimalValue(decimal);
        result.setHexValue(String.format("0x%02X", decimal));
        result.setBinaryValue(String.format("%8s", Integer.toBinaryString(decimal)).replace(' ', '0'));
        
        // 判断是否为可打印ASCII字符
        if (decimal >= 32 && decimal <= 126) {
            result.setAsciiValue(String.valueOf((char) decimal));
        } else {
            result.setAsciiValue(".");
        }
        
        List<Integer> byteList = new ArrayList<>();
        byteList.add(decimal);
        result.setByteList(byteList);
        
        return result;
    }

    /**
     * 14. 解析雷达数据（根据协议格式）
     * 假设协议格式: [帧头(2字节)] [距离(2字节)] [速度(1字节)] [角度(2字节)] [信号强度(1字节)] [校验(1字节)]
     */
    public RadarData parseRadarData(byte[] data, ByteOrderType order) {
        if (data == null || data.length < 8) {
            log.warn("数据长度不足，无法解析雷达数据: length={}", data == null ? 0 : data.length);
            return null;
        }
        
        RadarData radarData = new RadarData();
        
        try {
            // 帧头校验（示例：0x55AA）
            int header = twoBytesToDecimalBigEndian(data[0], data[1]);
            if (header != 0x55AA) {
                log.warn("无效的帧头: 0x{}", Integer.toHexString(header));
                return null;
            }
            
            // 解析距离（2字节）
            if (data.length > 3) {
                int distance = (order == ByteOrderType.BIG_ENDIAN) ?
                    twoBytesToDecimalBigEndian(data[2], data[3]) :
                    twoBytesToDecimalLittleEndian(data[2], data[3]);
                radarData.setDistance(distance);
            }
            
            // 解析速度（1字节）
            if (data.length > 4) {
                int speed = unsignedByteToDecimal(data[4]);
                radarData.setSpeed(speed);
            }
            
            // 解析角度（2字节）
            if (data.length > 6) {
                int angle = (order == ByteOrderType.BIG_ENDIAN) ?
                    twoBytesToDecimalBigEndian(data[5], data[6]) :
                    twoBytesToDecimalLittleEndian(data[5], data[6]);
                radarData.setAngle(angle);
            }
            
            // 解析信号强度（1字节）
            if (data.length > 7) {
                int strength = unsignedByteToDecimal(data[7]);
                radarData.setSignalStrength(strength);
            }
            
            // 保存原始数据
            radarData.getExtraData().put("rawData", bytesToHex(data));
            radarData.getExtraData().put("dataLength", data.length);
            
            log.debug("雷达数据解析成功: {}", radarData);
            
        } catch (Exception e) {
            log.error("解析雷达数据失败: {}", e.getMessage());
            return null;
        }
        
        return radarData;
    }

    /**
     * 15. 字节数组转十六进制字符串
     */
    public String bytesToHex(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X ", b & 0xFF));
        }
        return sb.toString().trim();
    }

    /**
     * 16. 十六进制字符串转字节数组
     */
    public byte[] hexToBytes(String hexString) {
        if (hexString == null || hexString.isEmpty()) {
            return new byte[0];
        }
        
        String hex = hexString.replaceAll("\\s", "");
        int len = hex.length();
        byte[] data = new byte[len / 2];
        
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        
        return data;
    }

    /**
     * 17. 打印字节数组详细信息
     */
    public void printByteArrayInfo(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            log.info("字节数组为空");
            return;
        }
        
        log.info("========== 字节数组解析信息 ==========");
        log.info("数据长度: {} 字节", bytes.length);
        log.info("十六进制: {}", bytesToHex(bytes));
        
        StringBuilder asciiBuilder = new StringBuilder();
        log.info("十进制详情:");
        for (int i = 0; i < bytes.length; i++) {
            int decimal = bytes[i] & 0xFF;
            char ascii = (decimal >= 32 && decimal <= 126) ? (char) decimal : '.';
            asciiBuilder.append(ascii);
            log.info("  [{}] 0x{} = {} (十进制), 二进制: {}, ASCII: {}",
                    i,
                    String.format("%02X", decimal),
                    decimal,
                    String.format("%8s", Integer.toBinaryString(decimal)).replace(' ', '0'),
                    ascii);
        }
        
        log.info("ASCII字符串: {}", asciiBuilder.toString());
        log.info("====================================");
    }

    /**
     * 18. 提取指定范围的字节
     */
    public byte[] extractBytes(byte[] source, int start, int length) {
        if (source == null || start < 0 || length <= 0 || start + length > source.length) {
            return new byte[0];
        }
        
        byte[] result = new byte[length];
        System.arraycopy(source, start, result, 0, length);
        return result;
    }

    /**
     * 19. 计算校验和（累加和）
     */
    public byte calculateChecksum(byte[] data) {
        int sum = 0;
        for (byte b : data) {
            sum += (b & 0xFF);
        }
        return (byte) (sum & 0xFF);
    }

    /**
     * 20. 验证校验和
     */
    public boolean verifyChecksum(byte[] data, byte expectedChecksum) {
        byte calculated = calculateChecksum(data);
        return calculated == expectedChecksum;
    }
}