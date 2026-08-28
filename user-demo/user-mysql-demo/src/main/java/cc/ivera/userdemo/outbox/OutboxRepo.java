package cc.ivera.userdemo.outbox;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface OutboxRepo extends MongoRepository<OutboxUserEvent, String> {

  @Query("{ 'status': { $in: ['NEW','RETRY','SENT'] }, 'nextRetryAt': { $lte: ?0 } }")
  List<OutboxUserEvent> findDue(LocalDateTime now);

  OutboxUserEvent findByEventId(String eventId);
}
