package com.lz.radar.utils;

import java.util.UUID;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 雪花算法ID生成器(静态工具类)
 */
public class SnowflakeIdGenerator {
    // 起始时间戳(2023-01-01)
    private static final long TWEPOCH = 1672531200000L;

    // 机器ID位数
    private static final long WORKER_ID_BITS = 5L;
    // 数据中心ID位数
    private static final long DATACENTER_ID_BITS = 5L;
    // 序列号位数
    private static final long SEQUENCE_BITS = 12L;

    // 最大机器ID (0-31)
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    // 最大数据中心ID (0-31)
    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

    // 机器ID左移位数
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    // 数据中心ID左移位数
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    // 时间戳左移位数
    private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

    // 序列号掩码 (4095)
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    // 工作机器ID(0-31)
    private static final long WORKER_ID;
    // 数据中心ID(0-31)
    private static final long DATACENTER_ID;

    // 序列号(0-4095)
    private static final AtomicLong SEQUENCE = new AtomicLong(0L);
    // 上次生成ID的时间戳
    private static volatile long LAST_TIMESTAMP = -1L;

    // 静态初始化
    static {
        // 这里可以从配置文件中读取，示例使用系统属性
        WORKER_ID = Long.getLong("snowflake.workerId", 1L);
        DATACENTER_ID = Long.getLong("snowflake.datacenterId", 1L);

        if (WORKER_ID > MAX_WORKER_ID || WORKER_ID < 0) {
            throw new IllegalArgumentException(
                    String.format("worker Id can't be greater than %d or less than 0", MAX_WORKER_ID));
        }
        if (DATACENTER_ID > MAX_DATACENTER_ID || DATACENTER_ID < 0) {
            throw new IllegalArgumentException(
                    String.format("datacenter Id can't be greater than %d or less than 0", MAX_DATACENTER_ID));
        }
    }

    // 私有构造方法防止实例化
    private SnowflakeIdGenerator() {}

    /**
     * 生成下一个ID
     * @return 雪花算法ID
     */
    public static long nextId() {
        long timestamp = timeGen();

        // 如果当前时间小于上一次ID生成的时间戳，说明系统时钟回退过
        if (timestamp < LAST_TIMESTAMP) {
            throw new RuntimeException(
                    String.format("Clock moved backwards. Refusing to generate id for %d milliseconds",
                            LAST_TIMESTAMP - timestamp));
        }

        // 如果是同一毫秒生成的，则进行序列号自增
        if (timestamp == LAST_TIMESTAMP) {
            long sequence = SEQUENCE.incrementAndGet() & SEQUENCE_MASK;
            // 序列号超出范围，等待下一毫秒
            if (sequence == 0) {
                timestamp = tilNextMillis(LAST_TIMESTAMP);
            }
        } else {
            // 时间戳改变，序列号重置
            SEQUENCE.set(0L);
        }

        LAST_TIMESTAMP = timestamp;

        // 生成ID
        return ((timestamp - TWEPOCH) << TIMESTAMP_LEFT_SHIFT) |
                (DATACENTER_ID << DATACENTER_ID_SHIFT) |
                (WORKER_ID << WORKER_ID_SHIFT) |
                SEQUENCE.get();
    }

    /**
     * 阻塞到下一个毫秒，直到获得新的时间戳
     */
    private static long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    /**
     * 返回当前时间(毫秒)
     */
    private static long timeGen() {
        return System.currentTimeMillis();
    }

    /**
     * 生成字符串形式的ID
     */
    public static String nextIdStr() {
        return Long.toString(nextId());
    }
}
