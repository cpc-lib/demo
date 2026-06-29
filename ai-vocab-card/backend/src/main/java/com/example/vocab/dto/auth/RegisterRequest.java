package com.example.vocab.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 2, max = 64, message = "用户名长度需在2-64个字符之间")
    private String username;

    @NotBlank(message = "用户编号不能为空")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9]*$", message = "用户编号只能包含英文字母和数字，且必须以字母开头")
    @Size(min = 2, max = 64, message = "用户编号长度需在2-64个字符之间")
    private String userCode;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度需在6-64个字符之间")
    private String password;

    @NotBlank(message = "确认密码不能为空")
    private String confirmPassword;
}
