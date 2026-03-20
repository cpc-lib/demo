package com.example.orderdemo.domain.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("t_user")
public class UserEntity {
  private Long id;
  private String username;
  private LocalDateTime createdAt;
}
