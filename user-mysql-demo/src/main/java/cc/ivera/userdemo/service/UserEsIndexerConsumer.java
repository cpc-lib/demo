package cc.ivera.userdemo.service;

import cc.ivera.userdemo.outbox.OutboxRepo;
import cc.ivera.userdemo.outbox.OutboxUserEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Locale;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.action.support.WriteRequest;
import org.elasticsearch.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserEsIndexerConsumer {

    private final RestHighLevelClient es;
    private final ObjectMapper om;
    private final OutboxRepo outboxRepo;

    // ✅ 可选：失败进入 DLQ，保证“不丢”
    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${app.es.index:user_search_v1}")
    private String index;

    @Value("${app.kafka.dlq:user-change-dlq-v1}")
    private String dlqTopic;

    @KafkaListener(topics = "${app.kafka.topic:user-change-v1}", containerFactory = "kafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> rec, Acknowledgment ack) {
        String eventId = null;
        try {
            log.info("kafka recv topic={} partition={} offset={} key={} value={}", rec.topic(), rec.partition(), rec.offset(), rec.key(), rec.value());

            // 1) 解析消息
            JsonNode root = om.readTree(rec.value());
            eventId = root.path("eventId").asText(null);

            String tenantId = root.path("tenantId").asText();
            long uid = root.path("uid").asLong();
            long version = root.path("version").asLong();
            JsonNode payload = root.path("payload");

            if (tenantId == null || tenantId.isEmpty() || uid <= 0 || payload == null || payload.isMissingNode()) {
                throw new IllegalArgumentException("bad kafka message: tenantId/uid/payload missing");
            }

            if (payload != null && payload.isObject()) {
                ObjectNode obj = (ObjectNode) payload;

                // 基础字段
                obj.put("updatedAt", OffsetDateTime.now(ZoneOffset.UTC).toString());
                obj.put("version", version);
                obj.put("tenantId", tenantId);
                obj.put("uid", String.valueOf(uid)); // keyword

                // ✅ phone_ngram：直接等于 phone（你的 analyzer=keyword+edge_ngram 会产生前缀 token）
                String phone = obj.path("phone").asText(null);
                if (phone != null && phone.trim().length() > 0) {
                    String digits = onlyDigits(phone);
                    // phone_edge analyzer 没 lower 也无所谓，主要是数字；建议写 digits 更干净
                    obj.put("phone_ngram", digits.isEmpty() ? phone : digits);
                }

                // ✅ email_ngram：等于 email 的 lowercase（email_edge analyzer 带 lowercase，但写入统一更稳）
                String email = obj.path("email").asText(null);
                if (email != null && email.trim().length() > 0) {
                    String low = email.trim().toLowerCase(Locale.ROOT);
                    obj.put("email", low);        // 你 mapping email 有 normalizer，写低更稳
                    obj.put("email_ngram", low);  // 关键：供模糊查询
                }
            }

            // 2) 索引不存在就报错（或你也可以自动创建 mapping）
            boolean exists = es.indices().exists(new org.elasticsearch.client.indices.GetIndexRequest(index),
                    RequestOptions.DEFAULT);
            if (!exists) {
                throw new IllegalStateException("ES index not found: " + index);
            }

            String docId = tenantId + ":" + uid;

            // 3) 强制写入（覆盖写），并 wait_until refresh，保证 Kibana 立刻查到
            BulkRequest br = new BulkRequest();
            br.setRefreshPolicy(WriteRequest.RefreshPolicy.WAIT_UNTIL);

            IndexRequest ir = new IndexRequest(index).id(docId).source(payload.toString(), XContentType.JSON);

            br.add(ir);

            BulkResponse resp = es.bulk(br, RequestOptions.DEFAULT);

            log.info("es bulk took={}ms items={} hasFailures={}", resp.getTook().getMillis(), resp.getItems().length, resp.hasFailures());

            if (resp.hasFailures()) {
                for (BulkItemResponse item : resp.getItems()) {
                    if (item.isFailed()) {
                        log.error("es bulk item failed id={} failure={}", item.getId(), item.getFailureMessage());
                    }
                }
                throw new RuntimeException(resp.buildFailureMessage());
            }

            // 4) 成功：回写 outbox ACK
            if (eventId != null && !eventId.isEmpty()) {
                OutboxUserEvent e = outboxRepo.findByEventId(eventId);
                if (e != null) {
                    e.setStatus("ACK");
                    e.setUpdatedAt(LocalDateTime.now());
                    outboxRepo.save(e);
                }
            }

            // 5) ack
            ack.acknowledge();

        } catch (Exception e) {
            log.error("consume failed topic={} partition={} offset={} err={}", rec.topic(), rec.partition(), rec.offset(), e.toString(), e);

            // ✅ 不丢：进 DLQ（你要“一定写入”，至少保证消息不丢）
            try {
                kafkaTemplate.send(dlqTopic, rec.key(), rec.value());
            } catch (Exception ignore) {
            }

            // 不 ack：让 Kafka 重试
        }
    }

    private String onlyDigits(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '0' && c <= '9') sb.append(c);
        }
        return sb.toString();
    }

}
