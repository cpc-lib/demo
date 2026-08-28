package com.example.articledelay.application;

public class ArticleNotFoundException extends RuntimeException {

    public ArticleNotFoundException(Long id) {
        super("Article not found: " + id);
    }
}
