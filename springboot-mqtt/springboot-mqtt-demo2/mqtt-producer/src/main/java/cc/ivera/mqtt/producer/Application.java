package cc.ivera.mqtt.producer;

import cc.ivera.mqtt.producer.handler.MqttMessageGateway;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * mqtt生产端
 */
@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        ConfigurableApplicationContext context =
                new SpringApplicationBuilder(Application.class)
                        .web(WebApplicationType.NONE)
                        .run(args);
        //发送的消息
        Message message = MessageBuilder.withPayload("send message to topic")
                //发送的主题
                .setHeader(MqttHeaders.TOPIC, "topic").build();
        MqttMessageGateway gateway = context.getBean(MqttMessageGateway.class);
        gateway.sendMessage(message);
    }
}
