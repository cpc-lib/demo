package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * 积分日志表：记录每次业务处理情况
 * status: 0=待处理,1=成功,2=失败
 */
@Data
public class PointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private String businessId;
    private Integer points;
    private Integer status;
    private Integer retryCount;
    private String errMsg;
    private Date createTime;
    private Date updateTime;
}
