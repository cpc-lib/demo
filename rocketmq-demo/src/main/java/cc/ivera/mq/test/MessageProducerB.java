package cc.ivera.mq.test;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

public class MessageProducerB {

    private static final String NAMESRV = "127.0.0.1:9876";
    private static final String TOPIC = "TestTopicA";
    private static final String PRODUCER_GROUP = "demo_producer_group";

    public static void main(String[] args) throws Exception {
        // 1. 创建生产者实例，并指定生产者组名
        DefaultMQProducer producer = new DefaultMQProducer(PRODUCER_GROUP);

        // 2. 设置NameServer地址
        producer.setNamesrvAddr(NAMESRV);

        // 3. 启动生产者实例
        producer.start();

        for (int i = 0; i < 10; i++) {
            // 4. 创建消息（主题topic、标签tag、内容body）
            Message msg = new Message("TestTopic", "TagA", ("Hello RocketMQ " + i).getBytes());
            
            // 5. 发送消息
            SendResult sendResult = producer.send(msg);
            System.out.printf("SendResult: %s%n", sendResult);
        }

        // 6. 关闭生产者
        producer.shutdown();
    }
}