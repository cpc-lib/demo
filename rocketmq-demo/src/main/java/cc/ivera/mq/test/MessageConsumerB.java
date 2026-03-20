package cc.ivera.mq.test;

import org.apache.rocketmq.client.consumer.DefaultMQPullConsumer;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.consumer.PullResult;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.message.MessageQueue;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MessageConsumerB {
    // 用于记录每个队列的消费偏移量
    /**
     * todo 保存到 数据库 topic,-queueId,brokerName
     */
    private static final Map<MessageQueue, Long> offsetTable = new ConcurrentHashMap<>();

    public static void main(String[] args) throws MQClientException, InterruptedException {
        // 1. 创建 Pull 消费者
        DefaultMQPullConsumer consumer = new DefaultMQPullConsumer("demo_pull_consumer_group");
        consumer.setNamesrvAddr("127.0.0.1:9876");
        consumer.start();

        // 2. 加载之前的消费进度（若有）
        loadOffset();

        // 3. 获取该 topic 下的队列
        Set<MessageQueue> mqs = consumer.fetchSubscribeMessageQueues("TestTopic");
        //MessageQueue next = mqs.iterator().next();

        System.out.println("当前Topic包含队列: " + mqs);

        // 4. 持续循环消费（拉取模式）
        while (true) {
            for (MessageQueue mq : mqs) {
                try {
                    System.out.println("**"+mq.getQueueId()+"-"+mq.getTopic()+"-"+mq.getBrokerName());
                    long offset = getMessageQueueOffset(mq);
                    PullResult pullResult = consumer.pull(mq, "*", offset, 32);

                    switch (pullResult.getPullStatus()) {
                        case FOUND:
                            List<MessageExt> messages = pullResult.getMsgFoundList();
                            for (MessageExt msg : messages) {
                                System.out.printf("拉取消息: queue=%s, offset=%d, body=%s%n",
                                        mq, msg.getQueueOffset(), new String(msg.getBody()));
                            }
                            // 更新 offset
                            putMessageQueueOffset(mq, pullResult.getNextBeginOffset());
                            break;
                        case NO_NEW_MSG:
                            // 没有新消息，可休眠一会
                            Thread.sleep(1000);
                            break;
                        case NO_MATCHED_MSG:
                        case OFFSET_ILLEGAL:
                            System.out.printf("拉取状态: %s%n", pullResult.getPullStatus());
                            break;
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            // 定期持久化 offset
            persistOffset();

            Thread.sleep(2000); // 每2秒拉取一次
        }
    }

    // 读取offset（可从文件、数据库、Redis中读取）
    private static void loadOffset() {
        File file = new File("offset.dat");
        if (!file.exists()) return;

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            Map<MessageQueue, Long> savedOffsets = (ConcurrentHashMap<MessageQueue, Long>) ois.readObject();
            offsetTable.putAll(savedOffsets);
            System.out.println("已加载上次消费进度: " + savedOffsets);
        } catch (Exception e) {
            System.err.println("读取offset失败: " + e.getMessage());
        }
    }

    // 保存offset（模拟持久化）
    private static void persistOffset() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("offset.dat"))) {
            oos.writeObject(offsetTable);
            System.out.println("消费进度已保存: " + offsetTable);
        } catch (IOException e) {
            System.err.println("保存offset失败: " + e.getMessage());
        }
    }

    private static long getMessageQueueOffset(MessageQueue mq) {
        Long offset = offsetTable.get(mq);
        return offset != null ? offset : 0L;
    }

    private static void putMessageQueueOffset(MessageQueue mq, long offset) {
        offsetTable.put(mq, offset);
    }
}