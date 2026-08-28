package com.example.articledelay.api;

import com.example.articledelay.application.ArticleService;
import com.example.articledelay.domain.ArticleStatus;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final ArticleService articleService;

    public ArticleController(ArticleService articleService) {
        this.articleService = articleService;
    }


    @GetMapping
    public ArticlePageResponse list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) ArticleStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return articleService.list(keyword, status, page, size);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ArticleResponse create(@Valid @RequestBody CreateArticleRequest request) {
        return articleService.createDraft(request.title(), request.content());
    }

    @GetMapping("/{id}")
    public ArticleResponse get(@PathVariable Long id) {
        return articleService.get(id);
    }

    @PostMapping("/{id}/schedule")
    public ArticleResponse schedule(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleArticleRequest request
    ) {
        return articleService.schedule(id, request.publishAt().toInstant());
    }

    @PostMapping("/{id}/cancel-schedule")
    public ArticleResponse cancelSchedule(@PathVariable Long id) {
        return articleService.cancelSchedule(id);
    }
}
