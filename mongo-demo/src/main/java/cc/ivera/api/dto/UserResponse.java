package cc.ivera.api.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserResponse {
    private String id;
    private String uid;
    private String phone;
    private String email;
    private String nickname;
    private Integer status;
    private Instant createdAt;
    private Instant updatedAt;
}
