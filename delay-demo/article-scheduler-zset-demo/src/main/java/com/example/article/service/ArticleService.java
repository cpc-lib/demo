package com.example.article.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.article.entity.Article;
import com.example.article.mapper.ArticleMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ArticleService {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String ZSET_KEY = "article:schedule";

    public Article createArticle(Article article) {
        // 保存为草稿
        article.setStatus(0);
        articleMapper.insert(article);

        schedulePublish(article);

        return article;
    }

    /**
     * 使用 Redis ZSet 安排定时发布
     */
    public void schedulePublish(Article article) {
        if (article.getPublishTime() == null) {
            return;
        }
        long timestamp = article.getPublishTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        stringRedisTemplate.opsForZSet()
                .add(ZSET_KEY, article.getId().toString(), timestamp);
    }

    /**
     * 取消定时发布任务
     */
    public void cancelSchedule(Long articleId) {
        stringRedisTemplate.opsForZSet()
                .remove(ZSET_KEY, articleId.toString());
    }

    /**
     * 实际发布文章（被调度任务调用）
     */
    public void publishArticle(Long articleId) {
        Article dbArticle = articleMapper.selectById(articleId);
        if (dbArticle == null) {
            return;
        }
        // 幂等：已经发布的不再重复
        if (dbArticle.getStatus() != null && dbArticle.getStatus() == 1) {
            return;
        }
        articleMapper.updateStatusToPublished(articleId);
    }

    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }

    public List<Article> listAll() {
        return articleMapper.selectList(new LambdaQueryWrapper<>());
    }
}
