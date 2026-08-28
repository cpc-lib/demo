package com.example.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.article.enums.ArticleStatus;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
@TableName("t_article")
public class ArticleEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String content;

    private Date publishTime;

    private ArticleStatus status;

    private Date createdAt;
    private Date updatedAt;
}
