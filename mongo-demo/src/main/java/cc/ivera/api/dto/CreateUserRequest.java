package cc.ivera.api.dto;

import lombok.Data;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
public class CreateUserRequest {

    @NotBlank
    @Size(max = 64)
    private String uid;

    @Size(max = 32)
    private String phone;

    @Email
    @Size(max = 128)
    private String email;

    @Size(max = 64)
    private String nickname;

    private Integer status = 1;
}
