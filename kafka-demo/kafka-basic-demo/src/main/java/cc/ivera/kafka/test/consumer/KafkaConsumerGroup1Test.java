package cc.ivera.kafka.test.consumer;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class KafkaConsumerGroup1Test {
    public static void main(String[] args) {

        // TODO 创建配置对象
        Map<String, Object> consumerConfig = new HashMap<>();
        consumerConfig.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop104:9092");
        consumerConfig.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        consumerConfig.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());

        consumerConfig.put(ConsumerConfig.GROUP_ID_CONFIG, "atguigu");
//        consumerConfig.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG, "aaa");
//        consumerConfig.put(ConsumerConfig.PARTITION_ASSIGNMENT_STRATEGY_CONFIG, StickyAssignor.class.getName());
        // TODO 创建消费者对象
        KafkaConsumer<String, String> consumer = new KafkaConsumer<String, String>(consumerConfig);

        // TODO 订阅主题
        consumer.subscribe(Arrays.asList("test"));

        // TODO 从kafka的主题中获取数据
        //      消费者从Kafka中拉取数据
        while ( true ) {
            final ConsumerRecords<String, String> datas = consumer.poll(Duration.ofMillis(100));

            for (ConsumerRecord<String, String> data : datas) {
                System.out.println(data.partition()+"key->"+data.key()+"value->"+data.value());
            }
        }

        // LEO : Log End Offset = size + 1 = 0,1,2,3, [4]
        // LEO = 3

        // TODO 关闭消费者对象
        //consumer.close();

    }
}
