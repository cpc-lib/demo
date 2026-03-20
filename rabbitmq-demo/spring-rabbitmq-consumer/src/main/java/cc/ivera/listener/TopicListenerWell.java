package cc.ivera.listener;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;

public class TopicListenerWell implements MessageListener {
    @Override
    public void onMessage(Message message) {
        System.out.println("topiclistenerwell:" +new String(message.getBody()));
    }
}
