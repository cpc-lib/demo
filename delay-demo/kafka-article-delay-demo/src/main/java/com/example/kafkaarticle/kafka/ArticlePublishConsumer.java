package com.example.kafkaarticle.kafka;

import com.example.kafkaarticle.service.ArticleService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ArticlePublishConsumer {

    @Resource
    private ArticleService articleService;

    /**
     * 最终执行的 Topic：真正发布文章
     */
    @KafkaListener(topics = "article-delay-final", groupId = "article-delay-group")
    public void onPublishMessage(String message) {
        String[] arr = message.split(",");
        Long articleId = Long.valueOf(arr[0]);

        articleService.publishArticle(articleId);
    }
}
