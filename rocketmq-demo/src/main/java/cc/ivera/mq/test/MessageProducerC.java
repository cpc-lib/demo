package cc.ivera.mq.test;

import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.common.message.MessageQueue;

import java.util.List;

public class MessageProducerC {
    public static void main(String[] args) throws Exception {

        //mqadmin updateTopic -n 127.0.0.1:9876 -b 127.0.0.1:10911 -t SingleQueueTopicA -r 1 -w 1

        // 1. 创建生产者
        DefaultMQProducer producer = new DefaultMQProducer("single_queue_producer_group");
        producer.setNamesrvAddr("127.0.0.1:9876");
        producer.start();

        // 2. 获取该 Topic 的所有队列
        List<MessageQueue> queues = producer.fetchPublishMessageQueues("SingleQueueTopicA");
        System.out.println("当前Topic队列列表: " + queues);

//        // ✅ 3. 选择固定队列（例如第一个 queueId=0）
        MessageQueue targetQueue = queues.get(0);//报错
        System.out.println("生产者绑定队列: " + targetQueue);

        // 4. 连续发送消息到该队列
        for (int i = 1; i <= 10; i++) {
            String body = "Message " + i;
            Message msg = new Message("SingleQueueTopicA", "TagA", body.getBytes());
            SendResult result = producer.send(msg, targetQueue);
            System.out.printf("发送成功 -> queueId=%d, msgId=%s, body=%s%n",targetQueue.getQueueId(), result.getMsgId(), body);

        }

        producer.shutdown();
        System.out.println("生产者已关闭。");
    }
}