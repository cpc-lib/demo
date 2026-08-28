package com.example.pointsdemo.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * 积分消息日志表：
 *  - 用户服务写入：status=0 待发送
 *  - 定时任务发送 MQ 后：status=1 已发送
 *  - 积分服务消费成功：status=2 已消费
 *  - 积分服务消费失败：status=3 失败
 */
@Data
public class PointsLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String businessId;

    private Integer points;

    /**
     * 0=待发送,1=已发送,2=已消费,3=消费失败
     */
    private Integer status;

    private Integer retryCount;

    private String errMsg;

    private Date createTime;

    private Date sendTime;

    private Date updateTime;
}