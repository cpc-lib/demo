package com.example.articlescheduler.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article_publish_mq_log")
public class ArticleMqLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String messageId;

    private Long articleId;

    private LocalDateTime createTime;
}
