package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.util.Date;

@Data
@TableName("user_points")
public class UserPoints {

    @TableId
    private Long id;

    private Long userId;

    private Integer points;

    private String reason;

    private Date createTime;

}
