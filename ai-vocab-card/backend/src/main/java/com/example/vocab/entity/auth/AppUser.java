package com.example.vocab.entity.auth;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("app_user")
public class AppUser {
    @TableId(type = IdType.AUTO) private Long id;
    private String username;
    private String userCode;
    private String passwordHash;
    private String nickname;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
