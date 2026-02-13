package cc.ivera.userdemo.controller.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class LoginReq {
  @NotBlank
  private String account;

  @NotBlank
  private String password;
}
