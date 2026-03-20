package cc.ivera;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:spring-rabbitmq-producer.xml")
public class ProducerTest {

    @Autowired
    private RabbitTemplate rabbitTemplate;


    /**
     * 简单模式
     */
    @Test
    public void testHelloWorld(){
        rabbitTemplate.convertAndSend("spring_queue","Hello world!");
    }


    /**
     * 工作队列
     */
    @Test
    public void testWorkQueue(){
        for (int i = 0; i < 5; i++) {
            rabbitTemplate.convertAndSend("work_queue",i+"-->work_queue_msg!");

        }
    }

    /**
     * fanout
     */
    @Test
    public void testFanout(){
        rabbitTemplate.convertAndSend("spring_fanout_exchange","","test_fanout");
    }


    /**
     * direct模式
     */
    @Test
    public void testDirect(){
        rabbitTemplate.convertAndSend("spring_direct_exchange","direct_route","test_direct");
    }



    /**
     * topic模式
     */
    @Test
    public void testTopic(){
        rabbitTemplate.convertAndSend("spring_topic_exchange","heima.news","你就是个垃圾");
    }

}
