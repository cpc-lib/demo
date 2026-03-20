package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_points_account")
public class PointsAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer points;
    private Integer version;
}
