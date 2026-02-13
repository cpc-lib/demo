package cc.ivera.userdemo.util;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 简化版 Snowflake（单机 demo 用）。
 * 生产建议把 workerId/datacenterId 做成可配置且保证唯一（或使用你已有的全局ID组件）。
 */
public class SnowflakeId {

  private static final long EPOCH = 1700000000000L; // 2023-11
  private final long workerId;
  private final long datacenterId;

  private long sequence = 0L;
  private long lastTs = -1L;

  public SnowflakeId(long workerId, long datacenterId) {
    if (workerId < 0 || workerId > 31) throw new IllegalArgumentException("workerId 0..31");
    if (datacenterId < 0 || datacenterId > 31) throw new IllegalArgumentException("datacenterId 0..31");
    this.workerId = workerId;
    this.datacenterId = datacenterId;
  }

  public synchronized String nextId() {
    long ts = System.currentTimeMillis();
    if (ts < lastTs) throw new IllegalStateException("Clock moved backwards");
    if (ts == lastTs) {
      sequence = (sequence + 1) & 4095; // 12 bits
      if (sequence == 0) {
        ts = waitNextMillis(ts);
      }
    } else {
      sequence = ThreadLocalRandom.current().nextInt(0, 4); // 轻微打散
    }
    lastTs = ts;

    long id = ((ts - EPOCH) << 22) | (datacenterId << 17) | (workerId << 12) | sequence;
    return Long.toUnsignedString(id);
  }

  private long waitNextMillis(long ts) {
    long t = System.currentTimeMillis();
    while (t <= ts) t = System.currentTimeMillis();
    return t;
  }
}
