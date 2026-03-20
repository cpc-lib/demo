package cc.ivera.listener;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

public class DirectListener implements MessageListener {
    @Override
    public void onMessage(Message message) {
        System.out.println("direct: "+new String(message.getBody()));
    }
}
