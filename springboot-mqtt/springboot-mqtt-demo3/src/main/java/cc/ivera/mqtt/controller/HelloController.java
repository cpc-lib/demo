package cc.ivera.mqtt.controller;

import cc.ivera.mqtt.service.IMqttSender;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试发送消息
 *
 * @author gong_yuzhuo
 */
@RestController
public class HelloController {

    /**
     * 注入发送MQTT的Bean
     */
    @Resource
    private IMqttSender imqttSender;

    /**
     * 发送自定义消息内容（使用默认主题）
     *
     * @param data
     */
    @RequestMapping("/test1/{data}")
    public Map<String, Object> test1(@PathVariable("data") String data) {
        imqttSender.sendToMqtt(data);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("message", "ok");
        return map;
    }

    /**
     * 发送自定义消息内容，且指定主题
     *
     * @param topic
     * @param data
     */
    @RequestMapping("/test2/{topic}/{data}")
    public Map<String, Object> test2(@PathVariable("topic") String topic, @PathVariable("data") String data) {
        imqttSender.sendToMqtt(topic, data);
        Map<String, Object> map = new HashMap<>();
        map.put("code", 200);
        map.put("message", "ok");
        return map;
    }

}
