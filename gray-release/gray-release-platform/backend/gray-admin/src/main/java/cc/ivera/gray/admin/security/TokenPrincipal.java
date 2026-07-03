package cc.ivera.gray.admin.security;

import java.util.List;

public record TokenPrincipal(String username, List<String> roles) {
}

