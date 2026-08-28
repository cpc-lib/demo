package cc.ivera.userdemo.mongo;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document("msg_dedup")
public class MsgDedupDoc {

  @Id
  private String eventId;

  @Indexed
  private String uid;

  private long version;

  private String topic;
  private Integer partition;
  private Long offset;

  private String status; // PROCESSING/DONE/DEAD
  private int retryCount;
  private Long nextRetryAt;
  private String lastError;

  private long createdAt;
  private long updatedAt;
}
