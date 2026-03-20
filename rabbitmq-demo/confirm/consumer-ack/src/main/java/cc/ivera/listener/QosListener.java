package cc.ivera.listener;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

/**
 * consumer 限流
 * 1.确保ask机制为手动
 * 2.listener-container 配置属性 prefetch = n, 表示每次拉去n条信息，经过手动确认之后再拉取n条信息
 */
@Component
public class QosListener implements ChannelAwareMessageListener {
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try{
            System.out.println(new String(message.getBody()));

            //处理业务逻辑
            System.out.println("consumer02----");

            //手动签收
            channel.basicAck(deliveryTag,true);
            Thread.sleep(1000);
        }catch(Exception e){
            //失败requeue
            channel.basicNack(deliveryTag,true,true);
        }
    }
}
