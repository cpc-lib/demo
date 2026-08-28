package cc.ivera.userdemo.cache;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录查找所需的最小快照：不包含敏感扩展字段。
 * pwdHash 仅用于校验密码（hash），不落 ES。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthSnapshot {
  private String uid;
  private String status;   // ACTIVE / CANCELED
  private String deleted;  // "0" / "1"
  private String pwdHash;
  private long version;
}
