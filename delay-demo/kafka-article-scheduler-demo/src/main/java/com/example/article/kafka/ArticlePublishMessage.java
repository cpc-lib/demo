package com.example.article.kafka;

import lombok.Data;

@Data
public class ArticlePublishMessage {
    private Long articleId;

    public ArticlePublishMessage() {
    }

    public ArticlePublishMessage(Long articleId) {
        this.articleId = articleId;
    }
}
