package cc.ivera.userdemo.service;

import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;

/**
 * Demo UID：时间戳 + 随机 + 自增缺失（演示用）
 * 生产请换成：Leaf/Segment/Snowflake（保证全局唯一且可扩展）
 */
@Component
public class UidGenerator {

  public long nextId() {
    long ts = System.currentTimeMillis();
    int r = ThreadLocalRandom.current().nextInt(1000, 9999);
    // 13位ts + 4位随机 -> 17~18位
    return ts * 10000 + r;
  }
}
