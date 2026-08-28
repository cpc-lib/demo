package com.example.pointsdemo.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

@Data
public class UserPoints {

    @TableId
    private Long userId;

    private Integer points;
}