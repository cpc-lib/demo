package cc.ivera.listener;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

public class WorkQueueListener1 implements MessageListener {
    @Override
    public void onMessage(Message message) {
        System.out.println("worklqueuelistener1:"+new String(message.getBody()));
    }
}
