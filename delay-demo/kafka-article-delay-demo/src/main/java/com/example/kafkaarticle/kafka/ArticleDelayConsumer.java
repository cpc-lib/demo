package com.example.kafkaarticle.kafka;

import com.example.kafkaarticle.service.ArticleDelayService;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ArticleDelayConsumer {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Resource
    private ArticleDelayService articleDelayService;

    /**
     * 处理所有延时 Topic：5m、1m、30s、5s
     */
    @KafkaListener(topics = {
            "article-delay-5m",
            "article-delay-1m",
            "article-delay-30s",
            "article-delay-5s"
    }, groupId = "article-delay-group")
    public void onDelayMessage(String message) {
        String[] arr = message.split(",");
        Long articleId = Long.valueOf(arr[0]);
        Long publishTimestamp = Long.valueOf(arr[1]);

        long now = System.currentTimeMillis();

        if (now >= publishTimestamp) {
            // 到时间了，推入最终执行 Topic
            kafkaTemplate.send("article-delay-final", message);
            System.out.println("[到期] 文章 " + articleId + " → 推入 article-delay-final");
            return;
        }

        long diff = publishTimestamp - now;
        String nextTopic;
        if (diff > 5 * 60 * 1000) {
            nextTopic = "article-delay-5m";
        } else if (diff > 60 * 1000) {
            nextTopic = "article-delay-1m";
        } else if (diff > 30 * 1000) {
            nextTopic = "article-delay-30s";
        } else if (diff > 5 * 1000) {
            nextTopic = "article-delay-5s";
        } else {
            nextTopic = "article-delay-final";
        }

        kafkaTemplate.send(nextTopic, message);
        System.out.println("[未到期] 文章 " + articleId + " 重新路由到 topic=" + nextTopic);
    }
}
