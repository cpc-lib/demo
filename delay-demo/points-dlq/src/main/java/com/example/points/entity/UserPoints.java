package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

/**
 * 用户总积分
 */
@Data
public class UserPoints {

    @TableId
    private Long userId;

    private Integer points;
}
