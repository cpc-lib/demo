package com.example.versioncache.cache;

import com.example.versioncache.entity.Article;
import com.example.versioncache.service.ArticleService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VersionedArticleCache {

    private final StringRedisTemplate redisTemplate;
    private final ArticleService articleService;
    private final ObjectMapper objectMapper;

    public VersionedArticleCache(StringRedisTemplate redisTemplate, ArticleService articleService, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.articleService = articleService;
        this.objectMapper = objectMapper;
    }

    private String key(Long id) {
        return "article:" + id;
    }

    /**
     * 读取
     */
    public Article getById(Long id) {
        String json = redisTemplate.opsForValue().get(key(id));
        if (StringUtils.hasText(json)) {
            try {
                CachedArticle wrapper = objectMapper.readValue(json, CachedArticle.class);
                return wrapper.getArticle();
            } catch (Exception ignored) {
            }
        }

        Article db = articleService.getById(id);
        if (db != null) {
            putCache(db);
        }
        return db;
    }

    /**
     * 新增
     */
    public Article createAndCache(Article article) {
        if (article.getDataVersion() == null) {
            article.setDataVersion(0L);
        }
        articleService.save(article);
        Article saved = articleService.getById(article.getId());
        putCache(saved);
        return saved;
    }

    /**
     * 更新 + 版本检查
     */
    public Article updateAndRefreshCache(Article req) {
        boolean ok = articleService.updateById(req);
        if (!ok) throw new RuntimeException("并发更新失败，请重试");

        Article fresh = articleService.getById(req.getId());

        if (fresh != null) putCacheVersionChecked(fresh);
        return fresh;
    }


    /**
     * 删除（单 key）
     */
    public boolean delete(Long id) {
        boolean ok = articleService.removeById(id);
        if (ok) {
            redisTemplate.delete(key(id));
        }
        return ok;
    }


    /**
     * 不带版本检查（首次写入）
     */
    private void putCache(Article article) {
        CachedArticle wrapper = new CachedArticle();
        wrapper.setVersion(article.getDataVersion());
        wrapper.setArticle(article);

        try {
            String json = objectMapper.writeValueAsString(wrapper);
            redisTemplate.opsForValue().set(key(article.getId()), json);
        } catch (Exception ignored) {
        }
    }

    /**
     * 带版本检查（防止旧请求覆盖新数据）
     */
    private void putCacheVersionChecked(Article article) {
        String key = key(article.getId());
        String json = redisTemplate.opsForValue().get(key);

        Long newVer = article.getDataVersion();
        Long oldVer = -1L;

        if (StringUtils.hasText(json)) {
            try {
                CachedArticle old = objectMapper.readValue(json, CachedArticle.class);
                oldVer = old.getVersion();
            } catch (Exception ignored) {
            }
        }

        if (oldVer > newVer) return; // 老请求丢弃

        putCache(article);
    }
}
