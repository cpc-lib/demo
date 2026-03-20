package cc.ivera.listener;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.stereotype.Component;

@Component
public class DlxListener implements ChannelAwareMessageListener {
    @Override
    public void onMessage(Message message, Channel channel) throws Exception {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try{
            System.out.println(new String(message.getBody()));

            //处理业务逻辑
            int i=1/0;
            //手动签收
            channel.basicAck(deliveryTag,true);
        }catch(Exception e){
            //失败requeue
            System.out.println("消息被拒收");
            channel.basicNack(deliveryTag,true,false);
        }

    }

}