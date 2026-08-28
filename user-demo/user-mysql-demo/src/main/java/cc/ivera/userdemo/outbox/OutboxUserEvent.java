package cc.ivera.userdemo.outbox;

import java.time.LocalDateTime;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("outbox_user_event")
public class OutboxUserEvent {
  @Id
  private String id;

  private String eventId;
  private String tenantId;
  private long uid;
  private String eventType; // USER_CREATED / USER_UPDATED / USER_DELETED
  private String payload;   // json
  private long version;

  private String status;    // NEW / SENT / ACK / DEAD
  private int retryCount;
  private LocalDateTime nextRetryAt;
  private String lastError;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
