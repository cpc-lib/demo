package com.example.article.service;

import cn.hutool.extra.spring.SpringUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.article.entity.Article;
import com.example.article.mapper.ArticleMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class ArticleService {

    @Resource
    private ArticleMapper articleMapper;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String PUBLISH_KEY_PREFIX = "article:publish:";

    public Article createArticle(Article article) {
        // 保存为草稿
        article.setStatus(0);
        articleMapper.insert(article);

        schedulePublish(article);

        return article;
    }

    /**
     * 重新设置定时发布（比如修改发布时间）
     */
    public void schedulePublish(Article article) {
        if (article.getPublishTime() == null) {
            return;
        }
        long delaySeconds = Duration.between(LocalDateTime.now(), article.getPublishTime()).getSeconds();
        if (delaySeconds < 0) {


            // 幂等处理：如果已经是已发布则不重复
            if (article.getStatus() != null && article.getStatus() == 1) {
                return;
            }
            articleMapper.updateStatusToPublished(article.getId());
            //delaySeconds = 0;
        } else {
            String key = PUBLISH_KEY_PREFIX + article.getId();
            stringRedisTemplate.opsForValue().set(key, "1", delaySeconds, TimeUnit.SECONDS);
        }

    }

    /**
     * 取消定时发布
     */
    public void cancelSchedule(Long articleId) {
        String key = PUBLISH_KEY_PREFIX + articleId;
        stringRedisTemplate.delete(key);
    }

    /**
     * 实际发布文章（被监听器调用）
     */
    public void publishArticle(Long articleId) {
        Article dbArticle = articleMapper.selectById(articleId);
        if (dbArticle == null) {
            return;
        }
        // 幂等处理：如果已经是已发布则不重复
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


    private ArticleService getSelf() {
        return SpringUtil.getBean(getClass());
    }
}
