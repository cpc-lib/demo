package cc.ivera.listener;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

public class WorkQueueListener2 implements MessageListener {
    @Override
    public void onMessage(Message message) {
        System.out.println("workqueuelistener2:"+new String(message.getBody()));
    }
}
