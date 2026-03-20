package com.example.points.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

/**
 * 人工处理表：记录需要人工介入的消息
 */
@Data
public class ManualProcess {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String businessId;
    private Long userId;
    private String errMsg;
    /** 0=未处理,1=已处理 */
    private Integer status;
    private Date createTime;
}
