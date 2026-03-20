package cc.ivera.listener;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

public class TopicListenerStar implements MessageListener {
    @Override
    public void onMessage(Message message) {
        System.out.println("topiclistenerStar:"+new String(message.getBody()));
    }
}
