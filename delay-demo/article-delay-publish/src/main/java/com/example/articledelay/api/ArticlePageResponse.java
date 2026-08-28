package com.example.articledelay.api;

import org.springframework.data.domain.Page;

import java.util.List;

public record ArticlePageResponse(
        List<ArticleResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ArticlePageResponse from(Page<ArticleResponse> page) {
        return new ArticlePageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
