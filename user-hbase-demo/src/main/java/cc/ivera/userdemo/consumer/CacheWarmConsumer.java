package cc.ivera.userdemo.consumer;

import cc.ivera.userdemo.cache.RedisProfileCacheService;
import cc.ivera.userdemo.domain.UserProfile;
import cc.ivera.userdemo.kafka.CacheWarmRequest;
import cc.ivera.userdemo.service.HBaseUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmConsumer {

  private final RedisProfileCacheService profileCache;
  private final HBaseUserService hbase;

  @KafkaListener(
      topics = "${app.kafka.topic.cache-warm:user-cache-warm}",
      groupId = "${app.kafka.group.cache-warm:cache-warm-worker}",
      containerFactory = "cacheWarmKafkaListenerContainerFactory"
  )
  public void onMessage(CacheWarmRequest req, Acknowledgment ack) throws Exception {
    if (req == null || req.getUid() == null || req.getUid().isBlank()) return;

    String uid = req.getUid();

    // 10秒去抖：避免被 C 端轮询打穿 HBase
    if (!profileCache.tryAcquireWarmLock(uid)) {
      return;
    }

    try {
      // 从 HBase 取权威用户（含逻辑删除状态）
      UserProfile p = hbase.getProfile(uid).orElse(null);
      if (p == null) {
        ack.acknowledge();
        // 不存在：短 TTL 空对象避免频繁 warm（这里简单不写）
        return;
      }

      // 逻辑删除/注销：删除 profile 缓存（避免继续返回）
      if (Objects.equals(p.getDeleted(), "1") || !"ACTIVE".equalsIgnoreCase(p.getStatus())) {
        profileCache.deleteProfile(uid);
        ack.acknowledge();
        return;
      }

      // 写入 Redis（profileCache 内部会 JSON 序列化 + TTL）
      profileCache.setProfile(p);
      ack.acknowledge();
    } catch (Exception e) {
      log.error("cache warm failed uid={}, reqId={}, err={}", uid, req.getReqId(), e.toString(), e);
      throw e;
    }
  }
}
