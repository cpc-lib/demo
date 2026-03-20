package cc.ivera.controller;

import com.alibaba.fastjson.JSON;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.model.pojo.Person;
import cc.ivera.util.KafkaProducerUtil;

import javax.annotation.Resource;

@RestController
@RequestMapping("kafka")
public class KafkaController {

    @Resource
    private KafkaProducerUtil kafkaProducerUtils;

    @PostMapping("/sendMsg")
    public void sendMsg() {
        Person student = new Person();
        student.setAge(18);
        student.setName("张三");
        String studentStr = JSON.toJSONString(student);
        this.kafkaProducerUtils.sendMessage("helloTopic", studentStr);
        System.out.println("生产消息：" + studentStr);
    }

    @KafkaListener(topics = "helloTopic")
    public void consumerMsg(String msg) {
        Person student = JSON.parseObject(msg, Person.class);
        System.out.println("消费此消息：" + student);
    }
}