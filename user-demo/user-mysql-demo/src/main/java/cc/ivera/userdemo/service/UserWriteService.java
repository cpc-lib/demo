package cc.ivera.userdemo.service;

import cc.ivera.userdemo.api.dto.CreateUserRequest;
import cc.ivera.userdemo.domain.UserBase;
import cc.ivera.userdemo.infra.Shards;
import cc.ivera.userdemo.outbox.OutboxRepo;
import cc.ivera.userdemo.outbox.OutboxUserEvent;
import cc.ivera.userdemo.repo.UserBaseMapper;
import cc.ivera.userdemo.repo.UserEmailIndexMapper;
import cc.ivera.userdemo.repo.UserPhoneIndexMapper;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class UserWriteService {

    private static final BCryptPasswordEncoder BCRYPT = new BCryptPasswordEncoder();

    private final UidGenerator uidGenerator;
    private final Shards shards;
    private final ObjectMapper om;
    private final StringRedisTemplate redis;

    private final UserBaseMapper userBaseMapper;
    private final UserPhoneIndexMapper phoneIndexMapper;
    private final UserEmailIndexMapper emailIndexMapper;
    private final OutboxRepo outboxRepo;
    private final KafkaTemplate kafkaTemplate;
    @Value("${app.kafka.topic:user-change-v1}")
    private  String topic;

    @Transactional
    public UserBase create(CreateUserRequest req) {

        // ✅ 0) 唯一性预检查（友好提示）
        String phoneTable = shards.phoneIndexTable(req.getPhone());
        Map<String, Object> ph = new HashMap<>();
        ph.put("table", phoneTable);
        ph.put("tenantId", req.getTenantId());
        ph.put("phone", req.getPhone());
        Long existUidByPhone = phoneIndexMapper.selectUidByPhone(ph);
        if (existUidByPhone != null && existUidByPhone > 0) {
            throw new IllegalArgumentException("phone already exists");
        }

        String emailTable = shards.emailIndexTable(req.getEmail());
        Map<String, Object> em = new HashMap<>();
        em.put("table", emailTable);
        em.put("tenantId", req.getTenantId());
        em.put("emailLower",req.getEmail().toLowerCase(Locale.ROOT));
        Long existUidByEmail = emailIndexMapper.selectUidByEmail(em);
        if (existUidByEmail != null && existUidByEmail > 0) {
            throw new IllegalArgumentException("email already exists");
        }


        long uid = uidGenerator.nextId();
        String tenantId = req.getTenantId();
        String emailLower = req.getEmail().toLowerCase(Locale.ROOT);

        UserBase ub = new UserBase();
        ub.setUid(uid);
        ub.setTenantId(tenantId);
        ub.setPhone(req.getPhone());
        ub.setEmail(emailLower);
        ub.setNickname(req.getNickname());
        ub.setGender(req.getGender());
        ub.setStatus("ACTIVE");
        ub.setDeleted(false);
        ub.setVersion(1);
        ub.setPasswordHash(BCRYPT.encode(req.getPassword()));

        // 1) 写 user_base 分表
        String baseTable = shards.userBaseTable(uid);
        Map<String, Object> p1 = new HashMap<>();
        p1.put("table", baseTable);
        p1.put("uid", ub.getUid());
        p1.put("tenantId", ub.getTenantId());
        p1.put("phone", ub.getPhone());
        p1.put("email", ub.getEmail());
        p1.put("nickname", ub.getNickname());
        p1.put("gender", ub.getGender());
        p1.put("status", ub.getStatus());
        p1.put("deleted", ub.isDeleted());
        p1.put("version", ub.getVersion());
        p1.put("passwordHash", ub.getPasswordHash());
        userBaseMapper.insert(p1);

        // 2) 写 phone/email 索引分表（唯一约束）
        String phone = shards.phoneIndexTable(ub.getPhone());
        Map<String, Object> p2 = new HashMap<>();
        p2.put("table", phone);
        p2.put("tenantId", tenantId);
        p2.put("phone", ub.getPhone());
        p2.put("uid", uid);
        phoneIndexMapper.upsert(p2);

        String email = shards.emailIndexTable(emailLower);
        Map<String, Object> p3 = new HashMap<>();
        p3.put("table", email);
        p3.put("tenantId", tenantId);
        p3.put("emailLower", emailLower);
        p3.put("uid", uid);
        emailIndexMapper.upsert(p3);

        // 3) 写 Mongo Outbox（用于异步写 ES）
        OutboxUserEvent evt = new OutboxUserEvent();
        evt.setEventId(UUID.randomUUID().toString().replace("-", ""));
        evt.setTenantId(tenantId);
        evt.setUid(uid);
        evt.setEventType("USER_CREATED");
        evt.setVersion(ub.getVersion());
        try {
            evt.setPayload(om.writeValueAsString(toSearchDoc(ub)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        evt.setStatus("NEW");
        evt.setRetryCount(0);
        evt.setNextRetryAt(LocalDateTime.now());
        evt.setCreatedAt(LocalDateTime.now());
        evt.setUpdatedAt(LocalDateTime.now());
        outboxRepo.save(evt);


        // 4) 预热 Redis（登录 200ms）
        redis.opsForValue().set("idx:uid:phone:" + tenantId + ":" + ub.getPhone(), String.valueOf(uid));
        redis.opsForValue().set("idx:uid:email:" + tenantId + ":" + emailLower, String.valueOf(uid));
        // auth snapshot
        try {
            redis.opsForValue().set("user:auth:" + tenantId + ":" + uid, om.writeValueAsString(ub));
        } catch (Exception ignore) {
        }

        // 返回不带 passwordHash
        ub.setPasswordHash(null);


        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    String msg = buildKafkaMessage(evt); // 见下方方法
                    kafkaTemplate.send(topic, String.valueOf(uid), msg).get();

                    // 发送成功 -> 标记 SENT（可选）
                    OutboxUserEvent db = outboxRepo.findByEventId(evt.getEventId());
                    if (db != null) {
                        db.setStatus("SENT");
                        db.setUpdatedAt(LocalDateTime.now());
                        outboxRepo.save(db);
                    }
                } catch (Exception ex) {
                    // 发送失败：保持 NEW/RETRY，让后续投递器补偿也行
                }
            }
        });

        return ub;
    }

    private String buildKafkaMessage(OutboxUserEvent e) {
        try {
            // payload 是 json 字符串 -> 读成树，避免双层转义
            Object payloadNode = om.readTree(e.getPayload());

            Map<String, Object> m = new HashMap<String, Object>();
            m.put("eventId", e.getEventId());
            m.put("tenantId", e.getTenantId());
            m.put("uid", e.getUid());
            m.put("eventType", e.getEventType());
            m.put("version", e.getVersion());
            m.put("payload", payloadNode);

            return om.writeValueAsString(m);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private Map<String, Object> toSearchDoc(UserBase ub) {
        Map<String, Object> m = new HashMap<>();
        m.put("tenantId", ub.getTenantId());
        m.put("uid", String.valueOf(ub.getUid()));
        m.put("phone", ub.getPhone());
        m.put("email", ub.getEmail());
        m.put("nickname", ub.getNickname());
        m.put("gender", ub.getGender());
        m.put("status", ub.getStatus());
        m.put("deleted", ub.isDeleted());
        m.put("version", ub.getVersion());
        m.put("updatedAt", java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).toString());
        return m;
    }
}
