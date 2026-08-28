package cc.ivera.userdemo.api.dto;

import javax.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
  @NotBlank
  private String tenantId;

  @NotBlank
  private String identifier; // phone or email

  @NotBlank
  private String password;
}
