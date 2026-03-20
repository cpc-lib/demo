package cc.ivera.mqtt;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class MqttController {

    @Autowired
    private MqttGateway mqttGateway;

    @RequestMapping("/send/{topic}/{message}")
    public String send(@PathVariable String topic, @PathVariable String message) {
        // 发送消息到指定topic
        mqttGateway.sendToMqtt(topic, message);
        return "send message : " + message;
    }
}