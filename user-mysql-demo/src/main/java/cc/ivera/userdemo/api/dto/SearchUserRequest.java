package cc.ivera.userdemo.api.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchUserRequest {

  //@NotBlank
  private String tenantId;

  /** 模糊关键字：phone/email/nickname 任意 */
  private String q;

  /** gender: M/F/U */
  private String gender;

  private String status;

  private Boolean deleted;

  @Min(1)
  @Max(200)
  private Integer pageSize = 20;

  /** search_after token（base64 or json string） */
  private String nextToken;
}
