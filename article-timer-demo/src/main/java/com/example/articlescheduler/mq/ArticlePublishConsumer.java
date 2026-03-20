package com.example.articlescheduler.mq;

import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.articlescheduler.entity.ArticleMqLog;
import com.example.articlescheduler.mapper.ArticleMapper;
import com.example.articlescheduler.mapper.ArticleMqLogMapper;
import lombok.RequiredArgsConstructor;
import org.apache.rocketmq.client.apis.ClientConfiguration;
import org.apache.rocketmq.client.apis.ClientException;
import org.apache.rocketmq.client.apis.ClientServiceProvider;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.consumer.FilterExpression;
import org.apache.rocketmq.client.apis.consumer.FilterExpressionType;
import org.apache.rocketmq.client.apis.consumer.PushConsumer;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class ArticlePublishConsumer {

    private final ArticleMapper articleMapper;
    private final ArticleMqLogMapper mqLogMapper;

    @Value("${rocketmq.endpoints:127.0.0.1:8081}")
    private String endpoints;

    @PostConstruct
    public void init() throws ClientException {
        ClientServiceProvider provider = ClientServiceProvider.loadService();
        ClientConfiguration configuration = ClientConfiguration.newBuilder().setEndpoints(endpoints).build();

        FilterExpression expression = new FilterExpression("*", FilterExpressionType.TAG);

        provider.newPushConsumerBuilder().setClientConfiguration(configuration).setConsumerGroup("consumer-group").setSubscriptionExpressions(Collections.singletonMap("article-publish", expression)).setMessageListener(this::handleMessage).build();
    }

    private ConsumeResult handleMessage(MessageView messageView) {

        try {
            String messageId = String.valueOf(messageView.getMessageId());

            // ✅ 正确读取 ByteBuffer 消息体
            ByteBuffer buffer = messageView.getBody();
            byte[] bytes = new byte[buffer.remaining()];
            buffer.get(bytes);
            String body = new String(bytes, StandardCharsets.UTF_8);

            System.out.println("收到消息 messageId=" + messageId + " body=" + body);

            // ✅ JSON 解析 articleId
            JSONObject json = JSONObject.parseObject(body);
            if (json == null) {
                System.err.println("消息体不是 JSON 格式，body=" + body);
                return ConsumeResult.SUCCESS;
            }

            Long articleId = json.getLong("articleId");
            if (articleId == null) {
                System.err.println("JSON 中没有 articleId 字段，body=" + body);
                return ConsumeResult.SUCCESS;
            }

            // === 幂等检查 ===
            boolean processed = mqLogMapper.selectCount(
                    new QueryWrapper<ArticleMqLog>().eq("message_id", messageId)
            ) > 0;
            if (processed) {
                System.out.println("重复消息，忽略 messageId=" + messageId);
                return ConsumeResult.SUCCESS;
            }

            // === 业务处理 ===
            int rows = articleMapper.publishArticle(articleId);

            // === 写入消息日志 ===
            ArticleMqLog log = new ArticleMqLog();
            log.setMessageId(messageId);
            log.setArticleId(articleId);
            try {
                mqLogMapper.insert(log);
            } catch (DuplicateKeyException ignore) {
            }

            if (rows > 0) {
                System.out.println("文章发布成功 articleId=" + articleId);
            } else {
                System.out.println("文章已发布（幂等） articleId=" + articleId);
            }

            return ConsumeResult.SUCCESS;

        } catch (Exception e) {
            // 一定要兜底，避免抛异常导致 RocketMQ 一直重试
            e.printStackTrace();
            return ConsumeResult.SUCCESS;
        }
    }


}
