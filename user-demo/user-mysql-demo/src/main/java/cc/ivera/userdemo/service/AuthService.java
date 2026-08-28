package cc.ivera.userdemo.service;

import cc.ivera.userdemo.api.dto.LoginRequest;
import cc.ivera.userdemo.api.dto.LoginResponse;
import cc.ivera.userdemo.domain.UserBase;
import cc.ivera.userdemo.infra.Shards;
import cc.ivera.userdemo.repo.UserBaseMapper;
import cc.ivera.userdemo.repo.UserEmailIndexMapper;
import cc.ivera.userdemo.repo.UserPhoneIndexMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

  private final StringRedisTemplate redis;
  private final ObjectMapper om;
  private final Shards shards;

  private final UserPhoneIndexMapper phoneIndexMapper;
  private final UserEmailIndexMapper emailIndexMapper;
  private final UserBaseMapper userBaseMapper;

  public LoginResponse login(LoginRequest req) {
    String tenantId = req.getTenantId();
    String identifier = req.getIdentifier().trim();
    boolean isEmail = identifier.contains("@");
    String idNorm = isEmail ? identifier.toLowerCase(Locale.ROOT) : identifier;

    long uid = resolveUidByIdentifier(tenantId, idNorm, isEmail);
    if (uid <= 0) {
      throw new IllegalArgumentException("user not found");
    }

    // 1) Redis 快取 auth snapshot（避免每次查 MySQL）
    String authKey = "user:auth:" + tenantId + ":" + uid;
    UserBase auth = null;
    String cached = redis.opsForValue().get(authKey);
    if (cached != null && !cached.isEmpty()) {
      try {
        auth = om.readValue(cached, UserBase.class);
      } catch (Exception ignore) {}
    }

    if (auth == null) {
      String table = shards.userBaseTable(uid);
      Map<String, Object> p = new HashMap<>();
      p.put("table", table);
      p.put("tenantId", tenantId);
      p.put("uid", uid);
      auth = userBaseMapper.selectAuthByUid(p);
      if (auth == null) {
        throw new IllegalArgumentException("user not found");
      }
      try {
        redis.opsForValue().set(authKey, om.writeValueAsString(auth), Duration.ofMinutes(30));
      } catch (Exception ignore) {}
    }

    if (auth.isDeleted() || !"ACTIVE".equalsIgnoreCase(auth.getStatus())) {
      throw new IllegalArgumentException("user disabled");
    }

    if (!BCRYPT.matches(req.getPassword(), auth.getPasswordHash())) {
      throw new IllegalArgumentException("bad credentials");
    }

    // 发一个 demo token（生产请换 JWT / session）
    String token = UUID.randomUUID().toString().replace("-", "");
    redis.opsForValue().set("sess:" + token, tenantId + ":" + uid, Duration.ofHours(12));

    return new LoginResponse(token, uid, auth.getNickname());
  }

  private long resolveUidByIdentifier(String tenantId, String identifier, boolean isEmail) {
    String cacheKey = (isEmail ? "idx:uid:email:" : "idx:uid:phone:") + tenantId + ":" + identifier;

    String v = redis.opsForValue().get(cacheKey);
    if (v != null && !v.isEmpty()) {
      try { return Long.parseLong(v); } catch (Exception ignore) {}
    }

    Long uid = null;
    if (isEmail) {
      String table = shards.emailIndexTable(identifier);
      Map<String, Object> p = new HashMap<>();
      p.put("table", table);
      p.put("tenantId", tenantId);
      p.put("emailLower", identifier);
      var idx = emailIndexMapper.selectByEmail(p);
      uid = (idx == null ? null : idx.getUid());
    } else {
      String table = shards.phoneIndexTable(identifier);
      Map<String, Object> p = new HashMap<>();
      p.put("table", table);
      p.put("tenantId", tenantId);
      p.put("phone", identifier);
      var idx = phoneIndexMapper.selectByPhone(p);
      uid = (idx == null ? null : idx.getUid());
    }

    if (uid != null && uid > 0) {
      redis.opsForValue().set(cacheKey, String.valueOf(uid), Duration.ofDays(1));
      return uid;
    }
    return -1;
  }
}
