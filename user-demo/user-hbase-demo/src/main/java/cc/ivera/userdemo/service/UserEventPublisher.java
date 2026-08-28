package cc.ivera.userdemo.service;

import cc.ivera.userdemo.config.AppProperties;
import cc.ivera.userdemo.event.UserEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserEventPublisher {

  private final KafkaTemplate<String, UserEvent> kafkaTemplate;
  private final AppProperties props;

  public void publishUpsert(String uid, long version, String op) {
    UserEvent evt = UserEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .op(op)
        .uid(uid)
        .version(version)
        .eventTime(System.currentTimeMillis())
        .build();

    kafkaTemplate.send(props.getKafka().getTopicUserUpserted(), uid, evt)
        .addCallback(
            ok -> log.info("kafka sent op={} uid={} ver={} eventId={}", op, uid, version, evt.getEventId()),
            ex -> log.error("kafka send failed op={} uid={} ver={} eventId={}", op, uid, version, evt.getEventId(), ex)
        );
  }
}
