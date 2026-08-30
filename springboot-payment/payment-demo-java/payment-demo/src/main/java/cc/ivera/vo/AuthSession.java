package cc.ivera.vo;

import cc.ivera.security.AuthUser;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthSession {

    private String accessToken;

    private long expiresIn;

    private AuthUser user;

    @JsonIgnore
    private String refreshToken;
}
