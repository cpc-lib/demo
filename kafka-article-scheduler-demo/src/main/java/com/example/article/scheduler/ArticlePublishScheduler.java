package com.example.article.scheduler;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.article.entity.ArticleEntity;
import com.example.article.enums.ArticleStatus;
import com.example.article.kafka.ArticlePublishMessage;
import com.example.article.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ArticlePublishScheduler {

    private final ArticleMapper articleMapper;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${app.kafka.topic.article-publish}")
    private String publishTopic;

    /**
     * 每 30 秒扫描一次，将到期未发布的文章发送到 Kafka 发布 Topic
     */
    @Scheduled(fixedDelay = 30000)
    public void scanAndSendPublishMessages() {
        LocalDateTime now = LocalDateTime.now();
        List<ArticleEntity> toPublish = articleMapper.selectList(
                new QueryWrapper<ArticleEntity>()
                        .eq("status", ArticleStatus.SCHEDULED)
                        .le("publish_time", now)
        );

        if (toPublish.isEmpty()) {
            return;
        }

        for (ArticleEntity a : toPublish) {
            ArticlePublishMessage msg = new ArticlePublishMessage(a.getId());
            kafkaTemplate.send(publishTopic, String.valueOf(a.getId()), msg);
            log.info("已将文章 {} 发送到 Kafka 发布 Topic {}", a.getId(), publishTopic);
        }
    }
}
