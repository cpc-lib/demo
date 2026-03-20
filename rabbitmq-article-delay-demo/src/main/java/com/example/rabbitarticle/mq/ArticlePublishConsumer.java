package com.example.rabbitarticle.mq;

import com.example.rabbitarticle.config.RabbitMqConfig;
import com.example.rabbitarticle.service.ArticleService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ArticlePublishConsumer {

    @Resource
    private ArticleService articleService;

    /**
     * 监听最终发布队列：消息从延时队列 TTL 到期后会被转发到这里
     */
    @RabbitListener(queues = RabbitMqConfig.PUBLISH_QUEUE)
    public void onPublishMessage(Long articleId) {
        articleService.publishArticle(articleId);
    }
}
