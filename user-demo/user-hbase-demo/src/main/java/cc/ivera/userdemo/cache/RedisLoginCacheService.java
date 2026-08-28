package cc.ivera.userdemo.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.*;

/**
 * 10亿用户账号查找加速：Redis 两次 GET
 * 1) account -> uid (phone/email)
 * 2) uid -> auth snapshot (status/deleted/pwdHash/version)
 *
 * 关键：由于“注销后手机号/邮箱可复用并生成新 uid”，删除/覆盖映射必须防乱序。
 * - set 映射：只接受更大 version（或更大 ts）
 * - del 映射：仅当当前 value 指向自己 uid 才删除（防误删新账号绑定）
 *
 * 存储兼容：
 * - 历史可能存的是纯 uid 字符串
 * - 新版本存 JSON：{uid, ver, ts}
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RedisLoginCacheService {

  private final StringRedisTemplate redis;
  private final ObjectMapper om;

  @Value("${app.login-cache.ttl-account-hours:168}") // 默认 7 天；也可以设 0 表示不设置 TTL（需自行改代码）
  private long ttlAccountHours;

  @Value("${app.login-cache.ttl-auth-minutes:10}")
  private long ttlAuthMinutes;

  private String kPhone(String phone) { return "a:phone:" + phone; }
  private String kEmail(String emailLower) { return "a:email:" + emailLower; }
  private String kAuth(String uid) { return "u:auth:" + uid; }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AccountBinding {
    private String uid;
    private long ver;
    private long ts;
  }

  // Lua：只有当新 ver 更大(或相等但 ts 更大)才覆盖；返回 1=成功，0=拒绝
  private static final DefaultRedisScript<Long> LUA_SET_IF_NEWER = new DefaultRedisScript<>();
  static {
    LUA_SET_IF_NEWER.setResultType(Long.class);
    LUA_SET_IF_NEWER.setScriptText(
        "local key = KEYS[1] " +
        "local newUid = ARGV[1] " +
        "local newVer = tonumber(ARGV[2]) " +
        "local newTs  = tonumber(ARGV[3]) " +
        "local ttlSec = tonumber(ARGV[4]) " +
        "local cur = redis.call('GET', key) " +
        "if (not cur) then " +
        "  if (ttlSec and ttlSec > 0) then redis.call('SET', key, ARGV[5], 'EX', ttlSec) else redis.call('SET', key, ARGV[5]) end " +
        "  return 1 " +
        "end " +
        // 兼容：cur 可能是纯 uid
        "local curUid = cur " +
        "local curVer = -1 " +
        "local curTs  = -1 " +
        "if (string.sub(cur,1,1) == '{') then " +
        "  local ok, obj = pcall(cjson.decode, cur) " +
        "  if ok and obj then " +
        "    curUid = obj['uid'] or curUid " +
        "    curVer = tonumber(obj['ver'] or -1) " +
        "    curTs  = tonumber(obj['ts']  or -1) " +
        "  end " +
        "end " +
        "if (newVer > curVer) or (newVer == curVer and newTs >= curTs) then " +
        "  if (ttlSec and ttlSec > 0) then redis.call('SET', key, ARGV[5], 'EX', ttlSec) else redis.call('SET', key, ARGV[5]) end " +
        "  return 1 " +
        "end " +
        "return 0"
    );
  }

  // Lua：仅当当前 value 指向 uid 才删除；返回 1=删了，0=没删
  private static final DefaultRedisScript<Long> LUA_DEL_IF_UID_MATCH = new DefaultRedisScript<>();
  static {
    LUA_DEL_IF_UID_MATCH.setResultType(Long.class);
    LUA_DEL_IF_UID_MATCH.setScriptText(
        "local key = KEYS[1] " +
        "local uid = ARGV[1] " +
        "local cur = redis.call('GET', key) " +
        "if (not cur) then return 0 end " +
        "local curUid = cur " +
        "if (string.sub(cur,1,1) == '{') then " +
        "  local ok, obj = pcall(cjson.decode, cur) " +
        "  if ok and obj then curUid = obj['uid'] or curUid end " +
        "end " +
        "if (curUid == uid) then redis.call('DEL', key); return 1 end " +
        "return 0"
    );
  }

  public Optional<String> getUidByPhone(String phone) {
    if (!StringUtils.hasText(phone)) return Optional.empty();
    return parseUid(redis.opsForValue().get(kPhone(phone)));
  }

  public Optional<String> getUidByEmail(String emailLower) {
    if (!StringUtils.hasText(emailLower)) return Optional.empty();
    return parseUid(redis.opsForValue().get(kEmail(emailLower)));
  }

  private Optional<String> parseUid(String raw) {
    if (!StringUtils.hasText(raw)) return Optional.empty();
    raw = raw.trim();
    if (!raw.startsWith("{")) {
      return Optional.of(raw); // 兼容旧格式：直接 uid
    }
    try {
      AccountBinding b = om.readValue(raw, AccountBinding.class);
      return StringUtils.hasText(b.getUid()) ? Optional.of(b.getUid()) : Optional.empty();
    } catch (Exception e) {
      // 解析失败时当作 uid
      return Optional.of(raw);
    }
  }

  /** 新推荐：带版本写入，避免乱序覆盖 */
  public boolean setUidByPhone(String phone, String uid, long ver, long tsMillis) {
    if (!StringUtils.hasText(phone) || !StringUtils.hasText(uid)) return false;
    AccountBinding b = new AccountBinding(uid, ver, tsMillis);
    String payload;
    try {
      payload = om.writeValueAsString(b);
    } catch (JsonProcessingException e) {
      log.warn("serialize binding failed, phone={}, uid={}, err={}", phone, uid, e.toString());
      payload = uid; // 降级：写纯 uid
    }
    long ttlSec = ttlAccountHours <= 0 ? 0 : Duration.ofHours(ttlAccountHours).getSeconds();
    Long r = redis.execute(LUA_SET_IF_NEWER, Collections.singletonList(kPhone(phone)),
        uid, String.valueOf(ver), String.valueOf(tsMillis), String.valueOf(ttlSec), payload);
    return r != null && r == 1L;
  }

  public boolean setUidByEmail(String emailLower, String uid, long ver, long tsMillis) {
    if (!StringUtils.hasText(emailLower) || !StringUtils.hasText(uid)) return false;
    AccountBinding b = new AccountBinding(uid, ver, tsMillis);
    String payload;
    try {
      payload = om.writeValueAsString(b);
    } catch (JsonProcessingException e) {
      log.warn("serialize binding failed, email={}, uid={}, err={}", emailLower, uid, e.toString());
      payload = uid;
    }
    long ttlSec = ttlAccountHours <= 0 ? 0 : Duration.ofHours(ttlAccountHours).getSeconds();
    Long r = redis.execute(LUA_SET_IF_NEWER, Collections.singletonList(kEmail(emailLower)),
        uid, String.valueOf(ver), String.valueOf(tsMillis), String.valueOf(ttlSec), payload);
    return r != null && r == 1L;
  }

  /** 兼容旧调用：不带版本（ver=0, ts=now） */
  public void setUidByPhone(String phone, String uid) {
    setUidByPhone(phone, uid, 0L, System.currentTimeMillis());
  }
  public void setUidByEmail(String emailLower, String uid) {
    setUidByEmail(emailLower, uid, 0L, System.currentTimeMillis());
  }

  /** 条件删除：只有当映射仍指向该 uid 才删（防误删新账号绑定） */
  public boolean delPhoneIfUidMatch(String phone, String uid) {
    if (!StringUtils.hasText(phone) || !StringUtils.hasText(uid)) return false;
    Long r = redis.execute(LUA_DEL_IF_UID_MATCH, Collections.singletonList(kPhone(phone)), uid);
    return r != null && r == 1L;
  }

  public boolean delEmailIfUidMatch(String emailLower, String uid) {
    if (!StringUtils.hasText(emailLower) || !StringUtils.hasText(uid)) return false;
    Long r = redis.execute(LUA_DEL_IF_UID_MATCH, Collections.singletonList(kEmail(emailLower)), uid);
    return r != null && r == 1L;
  }

  /** 仍保留“无条件删除”接口（仅后台运维/手工修复用） */
  public void delPhone(String phone) {
    if (!StringUtils.hasText(phone)) return;
    redis.delete(kPhone(phone));
  }
  public void delEmail(String emailLower) {
    if (!StringUtils.hasText(emailLower)) return;
    redis.delete(kEmail(emailLower));
  }

  public Optional<AuthSnapshot> getAuth(String uid) {
    if (!StringUtils.hasText(uid)) return Optional.empty();
    String json = redis.opsForValue().get(kAuth(uid));
    if (!StringUtils.hasText(json)) return Optional.empty();
    try {
      return Optional.of(om.readValue(json, AuthSnapshot.class));
    } catch (Exception e) {
      log.warn("redis auth snapshot parse failed, uid={}, err={}", uid, e.toString());
      return Optional.empty();
    }
  }

  public void setAuth(AuthSnapshot snap) {
    if (snap == null || !StringUtils.hasText(snap.getUid())) return;
    try {
      String json = om.writeValueAsString(snap);
      redis.opsForValue().set(kAuth(snap.getUid()), json, Duration.ofMinutes(ttlAuthMinutes));
    } catch (JsonProcessingException e) {
      log.warn("redis auth snapshot serialize failed, uid={}, err={}", snap.getUid(), e.toString());
    }
  }

  public void delAuth(String uid) {
    if (!StringUtils.hasText(uid)) return;
    redis.delete(kAuth(uid));
  }
}
