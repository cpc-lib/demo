package com.example.kafkaarticle.service;

import com.example.kafkaarticle.entity.Article;
import com.example.kafkaarticle.mapper.ArticleMapper;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.ZoneId;

@Service
public class ArticleDelayService {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    @Resource
    private ArticleMapper articleMapper;

    /**
     * 创建文章并推入 Kafka 延时队列
     */
    public Article createArticle(Article article) {
        // 先保存为草稿
        article.setStatus(0);
        articleMapper.insert(article);

        long publishTimestamp = article.getPublishTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        sendDelayMessage(article.getId(), publishTimestamp);

        return article;
    }

    /**
     * 根据剩余时间选择不同延时 Topic
     */
    public void sendDelayMessage(Long articleId, long publishTimestamp) {
        long diff = publishTimestamp - System.currentTimeMillis();

        String topic;
        if (diff > 5 * 60 * 1000) {
            topic = "article-delay-5m";
        } else if (diff > 60 * 1000) {
            topic = "article-delay-1m";
        } else if (diff > 30 * 1000) {
            topic = "article-delay-30s";
        } else if (diff > 5 * 1000) {
            topic = "article-delay-5s";
        } else {
            topic = "article-delay-final";
        }

        String payload = articleId + "," + publishTimestamp;
        kafkaTemplate.send(topic, payload);
        System.out.println("文章 " + articleId + " 推入延时队列 topic=" + topic + " payload=" + payload);
    }
}
