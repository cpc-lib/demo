package com.example.article.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String content;

    /**
     * 0 = 草稿, 1 = 已发布
     */
    private Integer status;

    /**
     * 计划发布时间
     */
    private LocalDateTime publishTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
