package com.example.article.controller;

import com.example.article.entity.Article;
import com.example.article.service.ArticleService;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Resource
    private ArticleService articleService;

    /**
     * 创建文章并定时发布
     */
    @PostMapping
    public Article create(@RequestBody ArticleCreateRequest request) {
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setPublishTime(request.getPublishTime());
        return articleService.createArticle(article);
    }

    /**
     * 查询单个
     */
    @GetMapping("/{id}")
    public Article get(@PathVariable Long id) {
        return articleService.getById(id);
    }

    /**
     * 列表
     */
    @GetMapping
    public List<Article> list() {
        return articleService.listAll();
    }

    @Data
    public static class ArticleCreateRequest {
        private String title;
        private String content;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime publishTime;
    }
}
