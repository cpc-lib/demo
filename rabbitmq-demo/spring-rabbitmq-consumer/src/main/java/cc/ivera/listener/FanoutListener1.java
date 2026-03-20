package cc.ivera.listener;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;


public class FanoutListener1 implements MessageListener {

    @Override
    public void onMessage(Message message) {
        System.out.println("fanout1: "+new String(message.getBody()));
    }
}
