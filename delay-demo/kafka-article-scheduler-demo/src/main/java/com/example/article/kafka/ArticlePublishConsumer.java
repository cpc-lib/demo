package com.example.article.kafka;

import com.example.article.service.ArticleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticlePublishConsumer {

    private final ArticleService articleService;

    @Value("${app.kafka.topic.article-publish}")
    private String publishTopic;

    @KafkaListener(topics = "${app.kafka.topic.article-publish}", groupId = "article-publish-consumer")
    @Transactional
    public void onMessage(ArticlePublishMessage msg) {
        log.info("收到文章发布消息，articleId={}", msg.getArticleId());
        articleService.markAsPublished(msg.getArticleId());
    }
}
