package com.example.orderdemo.infrastructure.id;

import java.util.concurrent.atomic.AtomicLong;

public class SnowflakeIdGenerator {

  // 简化版雪花：可用于 demo；生产可替换 Leaf/Snowflake 服务化
  private final long workerId;
  private final long datacenterId;

  private static final long EPOCH = 1700000000000L;

  private static final long WORKER_ID_BITS = 5L;
  private static final long DATACENTER_ID_BITS = 5L;
  private static final long SEQUENCE_BITS = 12L;

  private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
  private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS);

  private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
  private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
  private static final long TIMESTAMP_LEFT_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;

  private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

  private volatile long lastTimestamp = -1L;
  private final AtomicLong sequence = new AtomicLong(0);

  public SnowflakeIdGenerator(long workerId, long datacenterId) {
    if (workerId > MAX_WORKER_ID || workerId < 0) throw new IllegalArgumentException("workerId invalid");
    if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) throw new IllegalArgumentException("datacenterId invalid");
    this.workerId = workerId;
    this.datacenterId = datacenterId;
  }

  public synchronized long nextId() {
    long timestamp = timeGen();
    if (timestamp < lastTimestamp) {
      throw new IllegalStateException("Clock moved backwards.");
    }

    if (lastTimestamp == timestamp) {
      long seq = (sequence.incrementAndGet()) & SEQUENCE_MASK;
      if (seq == 0) {
        timestamp = tilNextMillis(lastTimestamp);
      }
    } else {
      sequence.set(0);
    }

    lastTimestamp = timestamp;

    return ((timestamp - EPOCH) << TIMESTAMP_LEFT_SHIFT)
        | (datacenterId << DATACENTER_ID_SHIFT)
        | (workerId << WORKER_ID_SHIFT)
        | (sequence.get() & SEQUENCE_MASK);
  }

  private long tilNextMillis(long lastTimestamp) {
    long ts = timeGen();
    while (ts <= lastTimestamp) {
      ts = timeGen();
    }
    return ts;
  }

  private long timeGen() {
    return System.currentTimeMillis();
  }
}
