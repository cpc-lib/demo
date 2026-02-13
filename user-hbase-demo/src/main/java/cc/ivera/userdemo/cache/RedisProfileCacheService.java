package cc.ivera.userdemo.cache;

import cc.ivera.userdemo.domain.UserProfile;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RedisProfileCacheService {

  private final StringRedisTemplate redis;
  private final ObjectMapper om;

  @Value("${app.profile-cache.ttl-minutes:30}")
  private long ttlMinutes;

  @Value("${app.profile-cache.warm-lock-seconds:10}")
  private long warmLockSeconds;

  private String kProfile(String uid) { return "u:profile:" + uid; }
  private String kWarmLock(String uid) { return "warm:profile:" + uid; }

  public Optional<String> getProfileJson(String uid) {
    if (!StringUtils.hasText(uid)) return Optional.empty();
    return Optional.ofNullable(redis.opsForValue().get(kProfile(uid)));
  }

  public void setProfile(UserProfile profile) {
    if (profile == null || !StringUtils.hasText(profile.getUid())) return;
    try {
      String json = om.writeValueAsString(profile);
      redis.opsForValue().set(kProfile(profile.getUid()), json, Duration.ofMinutes(ttlMinutes));
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
  }

  public void deleteProfile(String uid) {
    if (!StringUtils.hasText(uid)) return;
    redis.delete(kProfile(uid));
  }

  /** 10 秒内同 uid 只允许一次回源构建 */
  public boolean tryAcquireWarmLock(String uid) {
    if (!StringUtils.hasText(uid)) return false;
    Boolean ok = redis.opsForValue().setIfAbsent(kWarmLock(uid), "1", Duration.ofSeconds(warmLockSeconds));
    return ok != null && ok;
  }
}
