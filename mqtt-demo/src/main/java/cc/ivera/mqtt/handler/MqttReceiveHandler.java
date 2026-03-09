package cc.ivera.mqtt.handler;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class MqttReceiveHandler {

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void receive(Message<?> message) {

        String payload = message.getPayload().toString();

        System.out.println("收到MQTT消息: " + payload);

    }
}