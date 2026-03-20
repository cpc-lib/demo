package com.example.articlescheduler.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.articlescheduler.dto.ArticleCreateDTO;
import com.example.articlescheduler.entity.Article;
import com.example.articlescheduler.mapper.ArticleMapper;
import com.example.articlescheduler.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.message.Message;
import org.apache.rocketmq.client.apis.producer.Producer;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;

import static java.time.Instant.ofEpochMilli;

@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final Producer articleTimerProducer;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createArticle(ArticleCreateDTO dto) throws Exception {

        Article article = new Article();
        article.setTitle(dto.getTitle());
        article.setContent(dto.getContent());
        article.setPublishTime(dto.getPublishTime());
        article.setStatus(0);
        articleMapper.insert(article);

        long deliverTime = dto.getPublishTime().getTime(); // 时间 OK

        ClientServiceProvider provider = ClientServiceProvider.loadService();

        JSONObject json = new JSONObject();
        json.put("articleId", article.getId());

        Message message = provider.newMessageBuilder()
                .setTopic("article-publish")
                .setBody(json.toJSONString().getBytes(StandardCharsets.UTF_8))
                .setDeliveryTimestamp(deliverTime)
                .build();

        articleTimerProducer.send(message);

        return article.getId();
    }

    @Override
    public Article getById(Long id) {
        return articleMapper.selectOne(Wrappers.<Article>lambdaQuery().eq(Article::getId, id));
    }
}
