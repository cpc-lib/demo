package com.example.article.controller;

import com.example.article.dto.ArticleRequest;
import com.example.article.entity.ArticleEntity;
import com.example.article.service.ArticleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /**
     * 提交文章并设置定时发布时间
     */
    @PostMapping("/schedule")
    public ArticleEntity schedule(@RequestBody @Valid ArticleRequest req) {
        return articleService.scheduleArticle(req);
    }
}
