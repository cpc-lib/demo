package cc.ivera.userdemo.consumer;

import cc.ivera.userdemo.event.UserEvent;
import cc.ivera.userdemo.mongo.ManualTaskDoc;
import cc.ivera.userdemo.mongo.MsgDedupDoc;
import cc.ivera.userdemo.repo.ManualTaskRepository;
import cc.ivera.userdemo.repo.MsgDedupRepository;
import cc.ivera.userdemo.service.EsUserIndexService;
import cc.ivera.userdemo.cache.AuthSnapshot;
import cc.ivera.userdemo.cache.RedisLoginCacheService;
import cc.ivera.userdemo.service.HBaseUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEventConsumer {

    private final MsgDedupRepository dedupRepo;
    private final ManualTaskRepository manualRepo;
    private final HBaseUserService hbase;
    private final EsUserIndexService es;
    private final RedisLoginCacheService loginCache;
    private final cc.ivera.userdemo.config.AppProperties props;

    @KafkaListener(topics = "#{@appProperties.kafka.topicUserUpserted}", containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(@Payload UserEvent event, ConsumerRecord<String, UserEvent> record, Acknowledgment ack) {
        UserEvent evt = record.value();
        if (evt == null) {
            evt = event;
        }
        if (evt == null || evt.getEventId() == null) {
            ack.acknowledge();
            return;
        }

        long now = System.currentTimeMillis();
        MsgDedupDoc doc = new MsgDedupDoc();
        doc.setEventId(evt.getEventId());
        doc.setUid(evt.getUid());
        doc.setVersion(evt.getVersion());
        doc.setTopic(record.topic());
        doc.setPartition(record.partition());
        doc.setOffset(record.offset());
        doc.setStatus("PROCESSING");
        doc.setRetryCount(0);
        doc.setCreatedAt(now);
        doc.setUpdatedAt(now);

        try {
            dedupRepo.insert(doc);
            // 已处理/处理中：直接 ack（幂等）
            //ack.acknowledge();
        } catch (DuplicateKeyException dup) {
            // 已处理/处理中：直接 ack（幂等）
            ack.acknowledge();
            return;
        }

        try {
            processOnce(evt);
            // done
            doc.setStatus("DONE");
            doc.setUpdatedAt(System.currentTimeMillis());
            dedupRepo.save(doc);
            ack.acknowledge();
        } catch (Exception ex) {
            log.error("consume/process failed eventId={} uid={} ver={}", evt.getEventId(), evt.getUid(), evt.getVersion(), ex);
            // 失败：先记录，稍后由 RetryScheduler 扫描重试；这里也 ack 释放 Kafka 消费线程
            doc.setStatus("PROCESSING");
            doc.setRetryCount(1);
            doc.setLastError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            doc.setNextRetryAt(System.currentTimeMillis() + backoffMs(1));
            doc.setUpdatedAt(System.currentTimeMillis());
            dedupRepo.save(doc);
            ack.acknowledge();
        }
    }

    private void processOnce(UserEvent evt) throws IOException {
        Optional<cc.ivera.userdemo.domain.UserProfile> pOpt = hbase.getUser(evt.getUid());
        if (pOpt.isEmpty()) return;
        es.upsert(pOpt.get());
    }

    private long backoffMs(int retryCount) {
        // 指数退避：1->2s, 2->4s, 3->8s... 上限 60s
        long ms = (1L << Math.min(5, retryCount)) * 1000L;
        return Math.min(60000L, ms);
    }

    public void retry(MsgDedupDoc doc) throws Exception {
        UserEvent evt = UserEvent.builder().eventId(doc.getEventId()).uid(doc.getUid()).version(doc.getVersion()).op("UPSERT").eventTime(System.currentTimeMillis()).build();

        try {
            processOnce(evt);
            doc.setStatus("DONE");
            doc.setUpdatedAt(System.currentTimeMillis());
            dedupRepo.save(doc);
        } catch (Exception ex) {
            int next = doc.getRetryCount() + 1;
            doc.setRetryCount(next);
            doc.setLastError(ex.getClass().getSimpleName() + ": " + ex.getMessage());
            doc.setUpdatedAt(System.currentTimeMillis());

            if (next > props.getRetry().getMaxRetries()) {
                doc.setStatus("DEAD");
                doc.setNextRetryAt(null);
                dedupRepo.save(doc);

                // 写人工处理表
                ManualTaskDoc mt = new ManualTaskDoc();
                mt.setEventId(doc.getEventId());
                mt.setUid(doc.getUid());
                mt.setVersion(doc.getVersion());
                mt.setReason(doc.getLastError());
                mt.setStatus("PENDING");
                mt.setAttemptCount(0);
                mt.setCreatedAt(System.currentTimeMillis());
                mt.setUpdatedAt(System.currentTimeMillis());
                manualRepo.save(mt);

                log.warn("moved to manual_task eventId={} uid={} reason={}", doc.getEventId(), doc.getUid(), doc.getLastError());
                return;
            }

            doc.setNextRetryAt(System.currentTimeMillis() + backoffMs(next));
            dedupRepo.save(doc);
        }
    }
}
