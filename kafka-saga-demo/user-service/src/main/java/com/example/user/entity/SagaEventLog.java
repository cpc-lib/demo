package com.example.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_saga_event_log")
public class SagaEventLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String eventId;
    private String eventType;
    private String payload;
    private Integer status; // 0 待发送 / 待处理, 1 已处理, 2 失败
}
