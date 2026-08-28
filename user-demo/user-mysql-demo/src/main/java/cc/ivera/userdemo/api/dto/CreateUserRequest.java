package cc.ivera.userdemo.api.dto;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import lombok.Data;

@Data
public class CreateUserRequest {

  @NotBlank
  private String tenantId;

  @NotBlank
  @Pattern(regexp = "^[0-9]{11}$", message = "phone must be 11 digits")
  private String phone;

  @NotBlank
  @Email
  private String email;

  @NotBlank
  private String nickname;

  @NotBlank
  @Pattern(regexp = "^(M|F|U)$", message = "gender must be M/F/U")
  private String gender;

  @NotBlank
  private String password;
}
