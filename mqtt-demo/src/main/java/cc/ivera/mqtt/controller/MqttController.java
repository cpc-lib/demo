package cc.ivera.mqtt.controller;

import cc.ivera.mqtt.service.MqttPublishService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/mqtt")
public class MqttController {

    private final MqttPublishService mqttPublishService;

    public MqttController(MqttPublishService mqttPublishService) {
        this.mqttPublishService = mqttPublishService;
    }

    @GetMapping("/send")
    public String send(String msg) {

        mqttPublishService.publish("demo/topic", msg);

        return "发送成功";

    }
}