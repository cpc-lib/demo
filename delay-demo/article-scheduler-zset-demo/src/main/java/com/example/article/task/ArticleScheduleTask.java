package com.example.article.task;

import com.example.article.service.ArticleService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Set;

@Component
public class ArticleScheduleTask {

    private static final String ZSET_KEY = "article:schedule";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private ArticleService articleService;

    /**
     * 每秒轮询一次，检查到了发布时间的文章
     */
    @Scheduled(fixedRate = 5000)
    public void checkAndPublish() {
        long now = System.currentTimeMillis();

        // 获取所有到期的文章 ID（score <= now）
        Set<String> idSet = stringRedisTemplate.opsForZSet()
                .rangeByScore(ZSET_KEY, 0, now);

        if (idSet == null || idSet.isEmpty()) {
            return;
        }

        for (String idStr : idSet) {
            // 先从 ZSet 中移除，保证多节点下只有一个节点能成功移除并执行发布
            Long removed = stringRedisTemplate.opsForZSet()
                    .remove(ZSET_KEY, idStr);

            if (removed != null && removed > 0) {
                Long articleId = Long.valueOf(idStr);
                articleService.publishArticle(articleId);
                System.out.println("文章已定时发布，ID = " + articleId);
            }
        }
    }
}
