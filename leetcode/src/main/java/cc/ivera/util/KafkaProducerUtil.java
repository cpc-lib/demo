package cc.ivera.util;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

/**
 * kafka发送消息工具类
 */
@Component
public class KafkaProducerUtil {

    @Resource
    private KafkaTemplate<String, String> kafkaTemplate;

    /**
     * 发送消息
     *
     * @param topic   消息主题
     * @param msgBody 消息体
     */
    public void sendMessage(String topic, String msgBody) {
        this.kafkaTemplate.send(topic, msgBody);
    }
}