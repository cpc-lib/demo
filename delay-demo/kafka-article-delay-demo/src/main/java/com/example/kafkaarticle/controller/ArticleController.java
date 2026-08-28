package com.example.kafkaarticle.controller;

import com.example.kafkaarticle.entity.Article;
import com.example.kafkaarticle.service.ArticleDelayService;
import com.example.kafkaarticle.service.ArticleService;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/articles")
public class ArticleController {

    @Resource
    private ArticleDelayService articleDelayService;

    @Resource
    private ArticleService articleService;

    /**
     * 创建文章并定时发布（基于 Kafka 延时队列）
     */
    @PostMapping
    public Article create(@RequestBody ArticleCreateRequest request) {
        Article article = new Article();
        article.setTitle(request.getTitle());
        article.setContent(request.getContent());
        article.setPublishTime(request.getPublishTime());
        return articleDelayService.createArticle(article);
    }

    @GetMapping("/{id}")
    public Article get(@PathVariable Long id) {
        return articleService.getById(id);
    }

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
