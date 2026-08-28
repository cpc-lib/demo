package cc.ivera.userdemo.kafka;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CacheWarmPublisher {

  private final KafkaTemplate<String, CacheWarmRequest> cacheWarmKafkaTemplate;

  @Value("${app.kafka.topic.cache-warm:user-cache-warm}")
  private String topic;

  public String publishProfileWarm(String uid, String reason) {
    String reqId = UUID.randomUUID().toString();
    CacheWarmRequest msg = CacheWarmRequest.builder()
        .reqId(reqId)
        .uid(uid)
        .reason(reason)
        .want(Collections.singletonList("PROFILE"))
        .ts(System.currentTimeMillis())
        .build();

    cacheWarmKafkaTemplate.send(topic, uid, msg);
    return reqId;
  }
}
