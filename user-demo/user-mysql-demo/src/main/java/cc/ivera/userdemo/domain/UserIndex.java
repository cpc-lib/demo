package cc.ivera.userdemo.domain;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserIndex {
  private String tenantId;
  private String key; // phone/emailLower
  private long uid;
  private LocalDateTime updatedAt;
}
