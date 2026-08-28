package com.example.articlescheduler.controller;

import com.example.articlescheduler.dto.ArticleCreateDTO;
import com.example.articlescheduler.entity.Article;
import com.example.articlescheduler.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/articles")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    @PostMapping
    public Long create(@RequestBody @Validated ArticleCreateDTO dto) throws Exception {
        return articleService.createArticle(dto);
    }

    @GetMapping("/{id}")
    public Article get(@PathVariable Long id) {
        return articleService.getById(id);
    }
}
