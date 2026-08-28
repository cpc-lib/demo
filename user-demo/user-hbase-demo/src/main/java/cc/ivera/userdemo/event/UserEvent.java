package cc.ivera.userdemo.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserEvent {
  private String eventId;
  private String op;       // UPSERT / CANCELED
  private String uid;
  private long version;    // HBase cf:version
  private long eventTime;  // epoch ms
}
