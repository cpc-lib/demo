package com.example.rabbitarticle.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.rabbitarticle.entity.Article;
import com.example.rabbitarticle.mapper.ArticleMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

@Service
public class ArticleService {

    @Resource
    private ArticleMapper articleMapper;

    public Article getById(Long id) {
        return articleMapper.selectById(id);
    }

    public List<Article> listAll() {
        return articleMapper.selectList(new LambdaQueryWrapper<>());
    }

    public void publishArticle(Long articleId) {
        Article dbArticle = articleMapper.selectById(articleId);
        if (dbArticle == null) {
            return;
        }
        if (dbArticle.getStatus() == 1) {
            // 已发布，幂等
            return;
        }
        articleMapper.updateStatusToPublished(articleId);
        System.out.println("文章已发布，ID = " + articleId);
    }
}
