package cc.ivera.userdemo.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("user_es_manual_task")
public class ManualTaskDoc {

  @Id
  private String eventId;

  @Indexed
  private String uid;

  private long version;

  private String reason;
  private String status; // PENDING/FIXED/IGNORED

  private int attemptCount;
  private Long lastAttemptAt;

  private long createdAt;
  private long updatedAt;
}
