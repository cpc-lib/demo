package cc.ivera.ordermachine.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 订单号生成工具
 *
 * 号段格式：
 * prefix + yyyyMMddHHmmssSSS + machineId(3位) + sequence(4位)
 *
 * 示例：
 * ORD202604171230451231010001
 *
 * 特点：
 * 1. 支持业务前缀
 * 2. 支持多机器部署（machineId 区分）
 * 3. 同毫秒内支持最多 10000 个序号
 * 4. 趋势递增，便于排查问题
 */
public class OrderNoGenerator {

    /**
     * 时间格式：17位
     */
    private static final DateTimeFormatter TIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS");

    /**
     * 机器码，范围建议：0 ~ 999
     */
    private final int machineId;

    /**
     * 同一毫秒下的自增序列
     */
    private int sequence = 0;

    /**
     * 上次生成订单号的毫秒值
     */
    private long lastTimestamp = -1L;

    /**
     * 同一毫秒最多生成 10000 个
     */
    private static final int MAX_SEQUENCE = 9999;

    public OrderNoGenerator(int machineId) {
        if (machineId < 0 || machineId > 999) {
            throw new IllegalArgumentException("machineId 必须在 0~999 之间");
        }
        this.machineId = machineId;
    }

    /**
     * 生成默认普通订单号
     */
    public synchronized String nextOrderNo() {
        return nextOrderNo("ORD");
    }

    /**
     * 生成指定前缀订单号
     *
     * @param prefix 业务前缀，例如 ORD / PAY / REF
     */
    public synchronized String nextOrderNo(String prefix) {
        validatePrefix(prefix);

        long currentMillis = System.currentTimeMillis();

        // 时钟回拨保护
        if (currentMillis < lastTimestamp) {
            throw new IllegalStateException(
                    "检测到系统时钟回拨，拒绝生成订单号。lastTimestamp="
                            + lastTimestamp + ", currentMillis=" + currentMillis
            );
        }

        // 同一毫秒内递增
        if (currentMillis == lastTimestamp) {
            sequence++;
            if (sequence > MAX_SEQUENCE) {
                currentMillis = waitUntilNextMillis(lastTimestamp);
                sequence = 0;
            }
        } else {
            sequence = 0;
        }

        lastTimestamp = currentMillis;

        String timePart = formatMillis(currentMillis);
        String machinePart = String.format("%03d", machineId);
        String sequencePart = String.format("%04d", sequence);

        return prefix + timePart + machinePart + sequencePart;
    }

    private void validatePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            throw new IllegalArgumentException("prefix 不能为空");
        }
        if (!prefix.matches("[A-Z0-9]{2,10}")) {
            throw new IllegalArgumentException("prefix 仅支持 2~10 位大写字母或数字");
        }
    }

    private String formatMillis(long millis) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(millis),
                java.time.ZoneId.systemDefault()
        );
        return TIME_FORMATTER.format(dateTime);
    }

    private long waitUntilNextMillis(long lastTimestamp) {
        long currentMillis = System.currentTimeMillis();
        while (currentMillis <= lastTimestamp) {
            currentMillis = System.currentTimeMillis();
        }
        return currentMillis;
    }
}