package cc.ivera.springkafka.component;

import cc.ivera.springkafka.config.SpringBootKafkaConfig;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
public class KafkaDataConsumer {
    @KafkaListener(topics = SpringBootKafkaConfig.TOPIC_TEST, groupId = SpringBootKafkaConfig.GROUP_ID)
    public void topic_test(List<String> messages, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        for (String message : messages) {
            try{
                final JSONObject entries = JSONUtil.parseObj(message);
                System.out.println(SpringBootKafkaConfig.GROUP_ID + " 消费了： Topic:" + topic + ",Message:" + entries.getStr("data"));
            }catch (Exception e){
                log.warn("error.message:{},message:{}",e.getMessage(),message);
            }
            //ack.acknowledge();
        }
    }
}