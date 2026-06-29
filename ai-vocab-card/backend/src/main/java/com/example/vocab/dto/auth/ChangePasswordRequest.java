package com.example.vocab.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {
    @NotBlank(message = "旧密码不能为空")
    private String oldPassword;

    @NotBlank(message = "新密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6-64个字符之间")
    private String newPassword;

    @NotBlank(message = "确认新密码不能为空")
    private String confirmNewPassword;
}
