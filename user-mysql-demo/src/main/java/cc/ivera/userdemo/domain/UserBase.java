package cc.ivera.userdemo.domain;

import java.time.LocalDateTime;
import lombok.Data;

@Data
public class UserBase {
  private long uid;
  private String tenantId;
  private String phone;
  private String email;
  private String nickname;
  private String gender;
  private String status;
  private boolean deleted;
  private long version;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /** 仅用于写/登录校验 */
  private String passwordHash;
}
