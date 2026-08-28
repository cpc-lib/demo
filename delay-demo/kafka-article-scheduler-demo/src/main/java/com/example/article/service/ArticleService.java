package com.example.article.service;

import com.example.article.dto.ArticleRequest;
import com.example.article.entity.ArticleEntity;
import com.example.article.enums.ArticleStatus;
import com.example.article.mapper.ArticleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleMapper articleMapper;

    /**
     * 创建并调度文章（只负责存库，真正的定时发布由 Scheduler + Kafka 完成）
     */
    @Transactional
    public ArticleEntity scheduleArticle(ArticleRequest req) {
        ArticleEntity a = new ArticleEntity();
        a.setTitle(req.getTitle());
        a.setContent(req.getContent());
        a.setPublishTime(req.getPublishTime());
        a.setStatus(ArticleStatus.SCHEDULED);
        Date currentDate = new Date();
        a.setCreatedAt(currentDate);
        a.setUpdatedAt(currentDate);

        articleMapper.insert(a);
        log.info("文章 {} 已保存为待发布，发布时间 {}", a.getId(), a.getPublishTime());
        return a;
    }

    @Transactional
    public void markAsPublished(Long articleId) {
        ArticleEntity db = articleMapper.selectById(articleId);
        if (db == null) {
            log.warn("发布文章时未找到记录，id={}", articleId);
            return;
        }
        if (db.getStatus() == ArticleStatus.PUBLISHED) {
            log.info("文章 {} 已是 PUBLISHED 状态，忽略", articleId);
            return;
        }
        db.setStatus(ArticleStatus.PUBLISHED);
        db.setUpdatedAt(new Date());
        articleMapper.updateById(db);
        log.info("文章 {} 状态已更新为 PUBLISHED", articleId);
    }
}
