package cc.ivera.ttl.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.ttl.config.v3.DelayQueueConfig;

import javax.annotation.Resource;
import java.util.Date;

/**
 * 发送延迟消息
 */
@Slf4j
@RestController
@RequestMapping("/ttl")
public class SendMessageController {

    @Resource
    private RabbitTemplate rabbitTemplate;

    @GetMapping("sendmsg/{msg}")
    public void sendMsg(@PathVariable("msg") String msg) {
        log.info("当前时间:{},发送信息给两个TTL队列:{}", new Date(), msg);
        rabbitTemplate.convertAndSend("X", "XA", "消息来自ttl为10s：" + msg);
        rabbitTemplate.convertAndSend("X", "XB", "消息来自ttl为40s：" + msg);
    }

    /**
     * @param msg
     * @param ttlTime
     */
    @GetMapping("/sendttlandmsg/{msg}/{ttlTime}")
    public void sendMsg(@PathVariable("msg") String msg, @PathVariable("ttlTime") String ttlTime) {
        log.info("当前时间:{},发送时长为{}毫秒的信息给队列QC:{}", new Date(), ttlTime, msg);
        rabbitTemplate.convertAndSend("X", "XC", msg, message -> {
            //设置发送消息的延迟时间
            message.getMessageProperties().setExpiration(ttlTime);
            return message;
        });
    }


    /**
     * 基于插件：生产者，发送消息和延迟时间
     */
    @GetMapping("/sendDelaymsg/{msg}/{delayTime}")
    public void sendMsg(@PathVariable("msg") String msg, @PathVariable("delayTime") Integer delayTime) {
        log.info("当前时间:{},发送时长为{}毫秒的信息给延迟队列队列delayed.queue:{}", new Date(), delayTime, msg);
        rabbitTemplate.convertAndSend(DelayQueueConfig.DELAYED_EXCHANGE_NAME
                , DelayQueueConfig.DELAYED_ROUTING_KEY, msg, message -> {
                    //设置发送消息的延迟时间
                    message.getMessageProperties().setDelay(delayTime);
                    return message;
                });
    }



}
