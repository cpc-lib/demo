package cc.ivera.confirm.controller;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.confirm.config.RabbitConfig;

import javax.annotation.Resource;

@RestController
public class SendMessageController {

    @Resource
    RabbitTemplate rabbitTemplate;


    @GetMapping("/sendMsg")
    public void sendMessage() throws Exception
    {
        String message = "Hello world!";

        //这里故意将routingKey参数写入错误，让其应发确认消息送到队列失败回调
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME, "no_queue_name", message);

        //由于这里使用的是测试方法，当测试方法结束，RabbitMQ相关的资源也就关闭了，
        //会导致消息确认的回调出现问题，所有加段延时
        Thread.sleep(2000);
    }




}
