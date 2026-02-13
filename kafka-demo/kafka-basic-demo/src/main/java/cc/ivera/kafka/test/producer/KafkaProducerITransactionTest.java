package cc.ivera.kafka.test.producer;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

public class KafkaProducerITransactionTest {
    public static void main(String[] args) throws Exception {


        // TODO 创建配置对象
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop104:9092");
        // TODO 对生产的数据K, V进行序列化的操作
        configMap.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configMap.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        configMap.put(ProducerConfig.ACKS_CONFIG, "-1");
        configMap.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        configMap.put(ProducerConfig.RETRIES_CONFIG, 5);
        configMap.put(ProducerConfig.BATCH_SIZE_CONFIG, 5);
        configMap.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 3000);
        // TODO 事务ID，事务是基于幂等性操作
        configMap.put(ProducerConfig.TRANSACTIONAL_ID_CONFIG, "my-tx-id");

        KafkaProducer<String, String> producer = new KafkaProducer<String, String>(configMap);
        System.out.println("创建生产者对象");
        // TODO 初始化事务
        producer.initTransactions();
        System.out.println("初始化事务");
        try {

            // TODO 开启事务
            producer.beginTransaction();
            System.out.println("开启事务");
            for ( int i = 0; i < 10; i++ ) {
                ProducerRecord<String, String> record = new ProducerRecord<String, String>(
                        "test", "key" + i, "value" + i
                );

                final Future<RecordMetadata> send = producer.send(record);

            }
            System.out.println("生产数据完毕");
            // TODO 提交事务
            producer.commitTransaction();
            System.out.println("提交事务");
        } catch ( Exception e ) {
            e.printStackTrace();
            // TODO 中止事务
            producer.abortTransaction();
            System.out.println("中止事务");
        } finally {
            producer.close();
        }






    }
}
