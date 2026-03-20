package com.example.orderdemo.domain.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class CreateUserRequest {

  @NotBlank
  @Size(max = 64)
  private String username;

  @Size(max = 32)
  @Pattern(regexp = "^$|^[0-9+\\-]{6,32}$", message = "手机号格式不正确")
  private String phone;

  @Size(max = 128)
  private String email;
}
