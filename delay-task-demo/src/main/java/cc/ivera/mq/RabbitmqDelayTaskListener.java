package cc.ivera.mq;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Configuration;

/**
 * 微信公众号：三友的java日记
 *
 * @author sanyou
 * @date 2023/2/26 22:43
 */
@Configuration
@Slf4j
public class RabbitmqDelayTaskListener {

    @RabbitListener(queues = "sanyouDelayTaskQueue")
    public void onMessage(String msg) {
        log.info("接受到延迟任务:{}", msg);
    }

}
