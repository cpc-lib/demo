package com.example.orderdemo.no.impl;

/**
 * Snowflake（简化稳健版）
 * 结构：41bit 时间戳 + 5bit datacenter + 5bit worker + 12bit sequence
 * - 单机/多机都可用，前提：workerId/datacenterId 唯一且稳定
 */
public class SnowflakeIdWorker {

  /** 自定义纪元：2024-01-01 00:00:00 UTC */
  private static final long EPOCH = 1704067200000L;

  private static final long WORKER_ID_BITS = 5L;
  private static final long DATACENTER_ID_BITS = 5L;
  private static final long SEQUENCE_BITS = 12L;

  private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);       // 31
  private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS); // 31

  private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
  private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
  private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

  private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

  private final long workerId;
  private final long datacenterId;

  private long sequence = 0L;
  private long lastTimestamp = -1L;

  public SnowflakeIdWorker(long workerId, long datacenterId) {
    if (workerId < 0 || workerId > MAX_WORKER_ID) {
      throw new IllegalArgumentException("workerId out of range: " + workerId + ", max=" + MAX_WORKER_ID);
    }
    if (datacenterId < 0 || datacenterId > MAX_DATACENTER_ID) {
      throw new IllegalArgumentException("datacenterId out of range: " + datacenterId + ", max=" + MAX_DATACENTER_ID);
    }
    this.workerId = workerId;
    this.datacenterId = datacenterId;
  }

  public synchronized long nextId() {
    long timestamp = currentTime();

    // 时钟回拨保护：直接拒绝（生产可做更复杂的等待/容错策略）
    if (timestamp < lastTimestamp) {
      throw new IllegalStateException("Clock moved backwards. Refusing to generate id.");
    }

    if (timestamp == lastTimestamp) {
      sequence = (sequence + 1) & SEQUENCE_MASK;
      if (sequence == 0L) {
        timestamp = waitNextMillis(lastTimestamp);
      }
    } else {
      sequence = 0L;
    }

    lastTimestamp = timestamp;

    return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
        | (datacenterId << DATACENTER_ID_SHIFT)
        | (workerId << WORKER_ID_SHIFT)
        | sequence;
  }

  private long waitNextMillis(long lastTs) {
    long ts = currentTime();
    while (ts <= lastTs) {
      ts = currentTime();
    }
    return ts;
  }

  private long currentTime() {
    return System.currentTimeMillis();
  }
}
