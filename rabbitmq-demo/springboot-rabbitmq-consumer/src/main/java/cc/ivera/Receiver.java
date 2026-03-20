package cc.ivera;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class Receiver {

    @RabbitListener(queues="boot_queue")
    public void receiveMsg(Message msg) {
        System.out.println(new String(msg.getBody()));
    }

}
