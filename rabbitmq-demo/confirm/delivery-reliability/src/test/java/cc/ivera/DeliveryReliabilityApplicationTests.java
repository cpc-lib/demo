package cc.ivera;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring-rabbitmq-producer.xml")
public class DeliveryReliabilityApplicationTests {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * 确认模式：
     * connectionFactory中设置开启publisher-confirms
     * 在rabbitTemplate中定义confirmCallback回调函数
     */

    @Test
    public void testConfirm() {
        //定义回调
        rabbitTemplate.setConfirmCallback(new RabbitTemplate.ConfirmCallback() {
            //CorrelationData 配置信息
            //ack 交换机是否收到了消息 （失败，让消息再次发送）
            //cause 故障原因
            @Override
            public void confirm(CorrelationData correlationData, boolean ack, String cause) {
                System.out.println(ack);
                System.out.println("confirm...");
            }
        });
        //发送消息 （修改exchange）
        for (int i = 0; i < 3; i++) {
            rabbitTemplate.convertAndSend("test_exchange_confirm", "confirm", "message sent...");
        }
    }


    /**
     * 回退模式：当消息发送给exchange之后，exchange到queue失败才会执行
     * 开启回退模式
     * 设置ReturnCallback
     * 设置处理消息的模式
     * 消息路由未到queue，丢弃消息 default
     * 消息路由未到queue，返回消息发送方ReturnCallback
     */
    @Test
    public void testReturn() {
        //设置交换机消息失败处理消息的模式
        rabbitTemplate.setMandatory(true);
        //定义回调
        rabbitTemplate.setReturnCallback(new RabbitTemplate.ReturnCallback() {
            @Override
            public void returnedMessage(Message message, int replyCode, String replyText, String exchange, String routingKey) {
                System.out.println(message);
                System.out.println(replyCode);
                System.out.println(exchange);
                System.out.println(routingKey);
            }
        });
        //发送消息 （修改routingkey）
        for (int i = 0; i <= 100; i++) {
            rabbitTemplate.convertAndSend("test_exchange_confirm", "confirm", "message sent...");
        }
    }

    /***
     * TTL 消息过期时间
     * 设置队列过期时间使用参数 x-message-ttl，单位毫秒
     * 会对整个队列消息统一过期
     * 设置消息过期时间使用参数expiration，单位毫秒
     * 当该信息在队列头部时（消费时），会单独判断过期
     * 如果两者都设置了过期时间，以时间短的为准
     */

    @Test
    public void test01(){
        for (int i=0;i<10;i++){
            rabbitTemplate.convertAndSend("test_exchange_ttl","ttl.test","ttl message...");
        }
    }


    @Test
    public void test02(){
        rabbitTemplate.convertAndSend("test_exchange_ttl", "ttl.test", "ttl message...",
                new MessagePostProcessor() {
            @Override
            public Message postProcessMessage(Message message) throws AmqpException {
                //设置消息过期时间
                message.getMessageProperties().setExpiration("5000");
                //返回消息
                return message;
            }
        });
    }

    @Test
    public void test03(){
        rabbitTemplate.convertAndSend("test_exchange_ttl", "ttl.test", "ttl message...",
                new MessagePostProcessor() {
                    @Override
                    public Message postProcessMessage(Message message) throws AmqpException {
                        //设置消息过期时间
                        message.getMessageProperties().setExpiration("50000");
                        //返回消息
                        return message;
                    }
                });
    }


    /**
     * TTLListener
     */
    @Test
    public void test04(){
        for (int i = 0; i < 10; i++) {
            rabbitTemplate.convertAndSend("test_exchange_ttl","ttl.test","ttl message...");
            //if(i==7){
            //    rabbitTemplate.convertAndSend("test_exchange_ttl", "ttl.test", "ttl message..."+i,
            //            new MessagePostProcessor() {
            //                @Override
            //                public Message postProcessMessage(Message message) throws AmqpException {
            //                    //设置消息过期时间
            //                    message.getMessageProperties().setExpiration("30000");
            //                    //返回消息
            //                    return message;
            //                }
            //            });
            //}else{
            //    rabbitTemplate.convertAndSend("test_exchange_ttl","ttl.test","ttl message...");
            //}
        }
    }

    /***
     * Dead Letter Exchange --DlxListener
     */
    @Test
    public void testDlx(){

        //test_queue_dlx -- test.dlx.#

        //测试过期时间，死信队列
        rabbitTemplate.convertAndSend("test_exchange_dlx","test.dlx.normal","Who are you?");

        //测试消息队列消息数量限制
        for (int i = 0; i < 30; i++) {
            rabbitTemplate.convertAndSend("test_exchange_dlx","test.dlx.normal","Who are you?");
        }

        //测试拒收
        rabbitTemplate.convertAndSend("test_exchange_dlx","test.dlx.normal","Who are you?");

    }


    /**
     * DelayMsgListener
     */
    @Test
    public void testDelay(){
        //order_queue_dlx -- order.#
        rabbitTemplate.convertAndSend("order_exchange","order.msg","order_id:15579090800");
    }




}
