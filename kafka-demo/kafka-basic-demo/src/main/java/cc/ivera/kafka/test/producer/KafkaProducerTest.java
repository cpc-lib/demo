package cc.ivera.kafka.test.producer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import java.util.HashMap;
import java.util.Map;
public class KafkaProducerTest {
    public static void main(String[] args) {
        // TODO 配置属性集合
        Map<String, Object> configMap = new HashMap<>();
        // TODO 配置属性：Kafka服务器集群地址
        configMap.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop104:9092");
        // TODO 配置属性：Kafka生产的数据为KV对，所以在生产数据进行传输前需要分别对K,V进行对应的序列化操作
        configMap.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        configMap.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,
                "org.apache.kafka.common.serialization.StringSerializer");
        // TODO 创建Kafka生产者对象，建立Kafka连接
        //      构造对象时，需要传递配置参数
        KafkaProducer<String, String> producer = new KafkaProducer<>(configMap);
        // TODO 准备数据,定义泛型
        //      构造对象时需要传递 【Topic主题名称】，【Key】，【Value】三个参数
        ProducerRecord<String, String> record = new ProducerRecord<String, String>(
                "test", "key1", "value1"
        );
        // TODO 生产（发送）数据
        producer.send(record);
        // TODO 关闭生产者连接
        producer.close();
    }
}