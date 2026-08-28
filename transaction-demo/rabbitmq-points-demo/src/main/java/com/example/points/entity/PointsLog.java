package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("points_log")
public class PointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String businessId;
    private Integer points;
    private Integer status;      // 0=待处理 1=成功 2=失败
    private Integer retryCount;
    private String errMsg;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
