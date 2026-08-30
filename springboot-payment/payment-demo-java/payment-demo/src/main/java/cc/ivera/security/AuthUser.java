package cc.ivera.security;

import cc.ivera.enums.UserRole;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthUser {

    private Long userId;

    private String username;

    private UserRole role;
}
