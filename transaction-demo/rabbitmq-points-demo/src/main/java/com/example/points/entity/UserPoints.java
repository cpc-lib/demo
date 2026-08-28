package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_points")
public class UserPoints {

    @TableId(value = "user_id", type = IdType.INPUT)
    private Long userId;

    private Integer points;
    private LocalDateTime updateTime;

    @Version
    private Long version;
}
