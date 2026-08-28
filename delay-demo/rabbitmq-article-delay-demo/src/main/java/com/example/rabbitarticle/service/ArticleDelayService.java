package com.example.rabbitarticle.service;

import com.example.rabbitarticle.config.RabbitMqConfig;
import com.example.rabbitarticle.entity.Article;
import com.example.rabbitarticle.mapper.ArticleMapper;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;

@Service
public class ArticleDelayService {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @Resource
    private ArticleMapper articleMapper;

    /**
     * 创建文章并发送到延时队列
     */
    public Article createArticle(Article article) {
        // 1. 保存为草稿
        article.setStatus(0);
        articleMapper.insert(article);

        // 2. 计算延时毫秒数
        long delayMs = Duration.between(LocalDateTime.now(), article.getPublishTime()).toMillis();
        if (delayMs < 0) {
            delayMs = 0;
        }

        sendDelayMessage(article.getId(), delayMs);

        return article;
    }

    /**
     * 发送延时消息（依赖 TTL + 死信交换机）
     */
    public void sendDelayMessage(Long articleId, long delayMs) {
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.DELAY_EXCHANGE,
                RabbitMqConfig.DELAY_ROUTING_KEY,
                articleId,
                message -> {
                    // 设置消息的 TTL（单位：毫秒）
                    message.getMessageProperties().setExpiration(String.valueOf(delayMs));
                    return message;
                }
        );

        System.out.println("文章 ID=" + articleId + " 已进入延时队列，延迟(ms)=" + delayMs);
    }
}
