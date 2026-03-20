package com.example.article.listener;

import com.example.article.service.ArticleService;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ArticlePublishListener implements MessageListener {

    private static final String PUBLISH_KEY_PREFIX = "article:publish:";

    @Resource
    private ArticleService articleService;

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = message.toString();
        if (expiredKey.startsWith(PUBLISH_KEY_PREFIX)) {
            String idStr = expiredKey.substring(PUBLISH_KEY_PREFIX.length());
            try {
                Long articleId = Long.valueOf(idStr);
                articleService.publishArticle(articleId);
                System.out.println("文章定时发布成功，ID = " + articleId);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
    }
}
