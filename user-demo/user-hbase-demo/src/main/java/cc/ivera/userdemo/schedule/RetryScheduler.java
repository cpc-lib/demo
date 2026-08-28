package cc.ivera.userdemo.schedule;

import cc.ivera.userdemo.consumer.UserEventConsumer;
import cc.ivera.userdemo.repo.MsgDedupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RetryScheduler {

  private final MsgDedupRepository repo;
  private final UserEventConsumer consumer;

  @Scheduled(fixedDelayString = "#{@appProperties.retry.scanIntervalMs}")
  public void scanAndRetry() {
    long now = System.currentTimeMillis();
    var list = repo.findTop200ByStatusAndNextRetryAtLessThanEqualOrderByNextRetryAtAsc("PROCESSING", now);
    for (var doc : list) {
      try {
        consumer.retry(doc);
      } catch (Exception e) {
        log.error("retry failed eventId={} uid={}", doc.getEventId(), doc.getUid(), e);
      }
    }
  }
}
