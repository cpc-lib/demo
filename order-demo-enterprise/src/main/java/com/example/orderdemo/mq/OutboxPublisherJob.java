package com.example.orderdemo.mq;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.orderdemo.domain.entity.OutboxEventEntity;
import com.example.orderdemo.domain.enums.OutboxStatus;
import com.example.orderdemo.mapper.OutboxEventMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@EnableScheduling
@Component
public class OutboxPublisherJob {

  private final OutboxEventMapper outboxEventMapper;
  private final KafkaTemplate<String, String> kafkaTemplate;
  private final String topic;

  public OutboxPublisherJob(OutboxEventMapper outboxEventMapper,
                            KafkaTemplate<String, String> kafkaTemplate,
                            @Value("${app.kafka.topic}") String topic) {
    this.outboxEventMapper = outboxEventMapper;
    this.kafkaTemplate = kafkaTemplate;
    this.topic = topic;
  }

  @Scheduled(fixedDelay = 1000)
  public void publish() {
    List<OutboxEventEntity> batch = outboxEventMapper.selectList(new LambdaQueryWrapper<OutboxEventEntity>()
        .in(OutboxEventEntity::getStatus, OutboxStatus.NEW.name(), OutboxStatus.RETRY.name())
        .le(OutboxEventEntity::getNextRetryAt, LocalDateTime.now())
        .orderByAsc(OutboxEventEntity::getNextRetryAt)
        .last("limit 100"));

    for (OutboxEventEntity e : batch) {
      try {
        // key 用 orderId，保证同订单消息同分区（有序）
        kafkaTemplate.send(topic, String.valueOf(e.getAggregateId()), e.getPayloadJson()).get();
        markSent(e.getId());
        log.info("outbox sent id={}, agg={}", e.getId(), e.getAggregateId());
      } catch (Exception ex) {
        markRetry(e.getId(), e.getRetryCount() == null ? 0 : e.getRetryCount(), ex.getMessage());
      }
    }
  }

  @Transactional
  public void markSent(long outboxId) {
    OutboxEventEntity upd = new OutboxEventEntity();
    upd.setId(outboxId);
    upd.setStatus(OutboxStatus.SENT.name());
    upd.setUpdatedAt(LocalDateTime.now());
    outboxEventMapper.updateById(upd);
  }

//  @Transactional
//  public void markRetry(long outboxId, int retryCount, String reason) {
//    int next = retryCount + 1;
//    int backoffSeconds = Math.min(300, 2 * next); // 简单退避（生产可换指数退避）
//    OutboxEventEntity upd = new OutboxEventEntity();
//    upd.setId(outboxId);
//    upd.setStatus(OutboxStatus.RETRY.name());
//    upd.setRetryCount(next);
//    upd.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds));
//    upd.setUpdatedAt(LocalDateTime.now());
//    outboxEventMapper.updateById(upd);
//    log.warn("outbox retry id={}, retryCount={}, reason={}", outboxId, next, reason);
//  }

  @Transactional
  public void markRetry(long outboxId, int retryCount, String reason) {
    int next = retryCount + 1;

    // ---- 指数退避参数（可按业务调整）----
    int baseDelaySeconds = 2;     // 初始退避：2s
    int maxDelaySeconds = 300;    // 最大退避：5min
    int maxExp = 10;              // 防止位移溢出（2^10=1024倍，足够了）

    // 指数倍数：2^(next-1)，并做上限保护
    int exp = Math.min(next - 1, maxExp);
    long expDelay = (long) baseDelaySeconds << exp; // base * 2^exp

    long capped = Math.min(expDelay, maxDelaySeconds);

    // ---- 抖动 jitter：0% ~ 20%（避免同一批同时重试）----
    long jitter = ThreadLocalRandom.current().nextLong(0, Math.max(1, capped / 5 + 1)); // capped*0.2
    long backoffSeconds = Math.min(maxDelaySeconds, capped + jitter);

    OutboxEventEntity upd = new OutboxEventEntity();
    upd.setId(outboxId);
    upd.setStatus(OutboxStatus.RETRY.name());
    upd.setRetryCount(next);
    upd.setNextRetryAt(LocalDateTime.now().plusSeconds(backoffSeconds));
    upd.setUpdatedAt(LocalDateTime.now());
    outboxEventMapper.updateById(upd);

    log.warn("outbox retry id={}, retryCount={}, backoffSeconds={}, reason={}",
            outboxId, next, backoffSeconds, reason);
  }

}
