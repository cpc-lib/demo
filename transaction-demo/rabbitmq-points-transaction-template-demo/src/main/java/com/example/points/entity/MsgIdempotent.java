package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("msg_idempotent")
public class MsgIdempotent {

    @TableId
    private String msgId;

    private Integer status;

    private Date createTime;
}
