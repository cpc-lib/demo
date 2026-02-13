package cc.ivera.userdemo.api.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SearchUserResponse {
  private List<UserHit> list;
  private String nextToken;

  @Data
  @AllArgsConstructor
  public static class UserHit {
    private String tenantId;
    private String uid;
    private String phone;
    private String email;
    private String nickname;
    private String gender;
    private String status;
    private String updatedAt;
    private long version;
  }
}
