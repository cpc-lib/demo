package cc.ivera.userdemo.service;

import cc.ivera.userdemo.cache.AuthSnapshot;
import cc.ivera.userdemo.cache.RedisLoginCacheService;
import cc.ivera.userdemo.config.AppProperties;
import cc.ivera.userdemo.domain.UserProfile;
import cc.ivera.userdemo.repo.MsgDedupRepository;
import cc.ivera.userdemo.util.SnowflakeId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserFacadeService {

    private final HBaseUserService hbase;
    private final UserEventPublisher publisher;
    private final RedisLoginCacheService loginCache;
    private final AppProperties props;
    private final MsgDedupRepository dedupRepo;

    // demo：固定 worker/datacenter；生产务必保证每实例唯一
    private final SnowflakeId idGen = new SnowflakeId(1, 1);

    public String register(String phone, String email, String password) throws IOException {
        if (!StringUtils.hasText(phone) && !StringUtils.hasText(email)) {
            throw new IllegalArgumentException("phone/email required");
        }
        String uid = idGen.nextId();
        String emailLower = StringUtils.hasText(email) ? email.trim().toLowerCase() : null;

        boolean phoneLocked = false;
        boolean emailLocked = false;

        try {
            if (StringUtils.hasText(phone)) {
                phone = phone.trim();
                phoneLocked = hbase.acquirePhone(phone, uid);
                if (!phoneLocked) throw new IllegalStateException("PHONE_TAKEN");
            }
            if (StringUtils.hasText(emailLower)) {
                emailLocked = hbase.acquireEmail(emailLower, uid);
                if (!emailLocked) throw new IllegalStateException("EMAIL_TAKEN");
            }

            long now = System.currentTimeMillis();
            String pwdHash = HBaseUserService.bcrypt(password);
            hbase.putUser(uid, phone, emailLower, pwdHash, 1L, "ACTIVE", "0", now, now, null);

            publisher.publishUpsert(uid, 1L, "UPSERT");

            // 写穿登录缓存（立即可登录，避免等待异步消费）
            if (StringUtils.hasText(phone)) loginCache.setUidByPhone(phone, uid, 1L, now);
            if (StringUtils.hasText(emailLower)) loginCache.setUidByEmail(emailLower, uid, 1L, now);
            loginCache.setAuth(AuthSnapshot.builder()
                    .uid(uid)
                    .status("ACTIVE")
                    .deleted("0")
                    .pwdHash(pwdHash)
                    .version(1L)
                    .build());

            return uid;

        } catch (Exception e) {
            // 补偿释放（尽力而为）
            try {
                if (emailLocked) hbase.releaseEmail(emailLower, uid);
            } catch (Exception ignore) {
            }
            try {
                if (phoneLocked) hbase.releasePhone(phone, uid);
            } catch (Exception ignore) {
            }
            throw e;
        }
    }

    public String login(String account, String password) throws IOException {
        if (!StringUtils.hasText(account) || !StringUtils.hasText(password)) {
            throw new IllegalArgumentException("account/password required");
        }
        String acc = account.trim();

        // 1) account -> uid（优先 Redis，miss 才回源 HBase）
        Optional<String> uidOpt;
        if (acc.contains("@")) {
            String emailLower = acc.toLowerCase();
            uidOpt = loginCache.getUidByEmail(emailLower);
            if (!uidOpt.isPresent()) {
                uidOpt = hbase.uidByEmail(emailLower);
                uidOpt.ifPresent(uid -> loginCache.setUidByEmail(emailLower, uid));
            }
        } else {
            String phone = acc;
            uidOpt = loginCache.getUidByPhone(phone);
            if (!uidOpt.isPresent()) {
                uidOpt = hbase.uidByPhone(phone);
                uidOpt.ifPresent(uid -> loginCache.setUidByPhone(phone, uid));
            }
        }

        String uid = uidOpt.orElseThrow(() -> new IllegalArgumentException("ACCOUNT_NOT_FOUND"));

        // 2) uid -> auth snapshot（优先 Redis，miss 才回源 HBase）
        AuthSnapshot snap = loginCache.getAuth(uid).orElseGet(() -> {
            try {
                UserProfile p = hbase.getUser(uid).orElse(null);
                if (p == null) return null;
                String hash = hbase.getPwdHash(uid).orElse(null);
                AuthSnapshot s = AuthSnapshot.builder()
                        .uid(uid)
                        .status(p.getStatus())
                        .deleted(p.getDeleted())
                        .pwdHash(hash)
                        .version(p.getVersion())
                        .build();
                loginCache.setAuth(s);
                return s;
            } catch (IOException e) {
                return null;
            }
        });

        if (snap == null) throw new IllegalArgumentException("USER_NOT_FOUND");
        if (!"ACTIVE".equalsIgnoreCase(snap.getStatus()) || "1".equals(snap.getDeleted())) {
            throw new IllegalStateException("ACCOUNT_DISABLED");
        }
        if (!StringUtils.hasText(snap.getPwdHash())) {
            throw new IllegalArgumentException("NO_PASSWORD");
        }
        if (!HBaseUserService.bcryptMatches(password, snap.getPwdHash())) {
            throw new IllegalArgumentException("BAD_CREDENTIALS");
        }
        // demo：返回 uid 作为 token；生产换 JWT
        return uid;
    }

    public long updateContact(String uid, String newPhone, String newEmail) throws IOException {
        UserProfile old = hbase.getUser(uid).orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        if (!"ACTIVE".equalsIgnoreCase(old.getStatus()) || "1".equals(old.getDeleted())) {
            throw new IllegalStateException("ACCOUNT_DISABLED");
        }

        String phoneOld = old.getPhone();
        String emailOld = old.getEmail();
        String phoneNew = StringUtils.hasText(newPhone) ? newPhone.trim() : phoneOld;
        String emailNew = StringUtils.hasText(newEmail) ? newEmail.trim().toLowerCase() : emailOld;

        boolean lockedNewPhone = false;
        boolean lockedNewEmail = false;

        // 先抢新，再放旧
        try {
            if (phoneNew != null && !phoneNew.equals(phoneOld)) {
                lockedNewPhone = hbase.acquirePhone(phoneNew, uid);
                if (!lockedNewPhone) throw new IllegalStateException("PHONE_TAKEN");
            }
            if (emailNew != null && !emailNew.equals(emailOld)) {
                lockedNewEmail = hbase.acquireEmail(emailNew, uid);
                if (!lockedNewEmail) throw new IllegalStateException("EMAIL_TAKEN");
            }

            long newVer = hbase.bumpVersionAndUpdateContact(uid, phoneNew, emailNew);
            // 释放旧
            if (phoneOld != null && !phoneOld.equals(phoneNew)) hbase.releasePhone(phoneOld, uid);
            if (emailOld != null && !emailOld.equals(emailNew)) hbase.releaseEmail(emailOld, uid);

            publisher.publishUpsert(uid, newVer, "UPSERT");

            // 写穿登录缓存（账号映射与快照）
            if (phoneOld != null && !phoneOld.equals(phoneNew)) loginCache.delPhoneIfUidMatch(phoneOld, uid);
            if (emailOld != null && !emailOld.equalsIgnoreCase(emailNew))
                loginCache.delEmailIfUidMatch(emailOld.toLowerCase(), uid);
            if (StringUtils.hasText(phoneNew))
                loginCache.setUidByPhone(phoneNew, uid, newVer, System.currentTimeMillis());
            if (StringUtils.hasText(emailNew))
                loginCache.setUidByEmail(emailNew, uid, newVer, System.currentTimeMillis());
            // auth 快照：直接失效，让下一次登录回源填充（或你也可以在这里回查一次 HBase 再 setAuth）
            loginCache.delAuth(uid);

            return newVer;

        } catch (Exception e) {
            // 回滚：释放抢到的新占位（主表未写成功前才会走到这里；主表写成功后我们不回滚，只做补偿任务）
            try {
                if (lockedNewEmail) hbase.releaseEmail(emailNew, uid);
            } catch (Exception ignore) {
            }
            try {
                if (lockedNewPhone) hbase.releasePhone(phoneNew, uid);
            } catch (Exception ignore) {
            }
            throw e;
        }
    }

    public long cancel(String uid) throws IOException {
        UserProfile u = hbase.getUser(uid).orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        if (!"ACTIVE".equalsIgnoreCase(u.getStatus()) || "1".equals(u.getDeleted())) {
            throw new IllegalStateException("ALREADY_CANCELED");
        }
        long newVer = hbase.bumpVersionAndMarkCanceled(uid);

        // 释放唯一索引（释放失败不影响一致性，后续可加补偿任务）
        if (u.getPhone() != null) hbase.releasePhone(u.getPhone(), uid);
        if (u.getEmail() != null) hbase.releaseEmail(u.getEmail().toLowerCase(), uid);

        publisher.publishUpsert(uid, newVer, "CANCELED");

        // 写穿登录缓存（注销：允许 phone/email 复用并生成新 uid）
        if (u.getPhone() != null) loginCache.delPhoneIfUidMatch(u.getPhone(), uid);
        if (u.getEmail() != null) loginCache.delEmailIfUidMatch(u.getEmail().toLowerCase(), uid);
        loginCache.delAuth(uid);

        return newVer;
    }
}
