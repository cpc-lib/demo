package cc.ivera.userdemo.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class RegisterReq {
  private String phone;
  private String email;

  @NotBlank
  private String password;
}
