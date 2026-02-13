package cc.ivera.userdemo.controller;

import cc.ivera.userdemo.cache.RedisProfileCacheService;
import cc.ivera.userdemo.kafka.CacheWarmPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class MeController {

  private final RedisProfileCacheService profileCache;
  private final CacheWarmPublisher cacheWarmPublisher;

  /**
   * C端：只读 Redis，不回源 HBase。
   * - 命中：200 + profile json
   * - 未命中：发送 Kafka warm 任务 + 202（缓存构建中）
   *
   * 说明：示例用 X-UID 传 uid；生产建议从 JWT/Session 解析 uid。
   */
  @GetMapping("/me")
  public ResponseEntity<?> me(@RequestHeader("X-UID") String uid) {
    return profileCache.getProfileJson(uid)
        .<ResponseEntity<?>>map(json -> ResponseEntity.ok().header("X-Cache", "HIT").body(json))
        .orElseGet(() -> {
          String reqId = cacheWarmPublisher.publishProfileWarm(uid, "ME_MISS");
          return ResponseEntity.accepted()
              .header("X-Cache", "MISS")
              .body(Map.of(
                  "code", "CACHE_WARMING",
                  "message", "缓存构建中，请稍后重试",
                  "uid", uid,
                  "requestId", reqId
              ));
        });
  }
}
