package cc.ivera.userdemo.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserProfile {
  private String uid;
  private String phone;
  private String email;
  private String status; // ACTIVE/CANCELED
  private String deleted; // 0/1
  private long version;
  private long createdAt;
  private long updatedAt;
  private Long canceledAt;
}
