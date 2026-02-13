package cc.ivera.springkafka.controller;

import cc.ivera.springkafka.config.SpringBootKafkaConfig;
import lombok.extern.slf4j.Slf4j;
import cn.hutool.json.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/kafka")
@Slf4j
public class KafkaProducerController {


    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    @ResponseBody
    @PostMapping(value = "/produce", produces = "application/json")
    public String produce(@RequestBody Object obj) {
        try {
            String obj2String = JSONUtil.toJsonStr(obj);
            kafkaTemplate.send(SpringBootKafkaConfig.TOPIC_TEST, obj2String);
            return "success";
        } catch (Exception e) {
            e.getMessage();
        }
        return "success";
    }
}