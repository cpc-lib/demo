package cc.ivera;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@SpringBootTest
@RunWith(SpringRunner.class)
class SpringbootRabbitmqProducerApplicationTests {

    //路由，交换机 topic 规则 *,#


    @Resource
    private RabbitTemplate rabbitTemplate;

    @Test
    void contextLoads() {
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE_NAME,"boot.hello","Talk is cheap,show me the code!");
    }

}
