package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("dead_message")
public class DeadMessage {

    @TableId
    private Long id;

    private String msgId;

    private Long userId;

    private String payload;

    private String reason;

    private Date createTime;
}
