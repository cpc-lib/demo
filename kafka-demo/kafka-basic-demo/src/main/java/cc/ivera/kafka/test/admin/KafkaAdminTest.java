package cc.ivera.kafka.test.admin;

import org.apache.kafka.clients.admin.Admin;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;

import java.util.*;
public class KafkaAdminTest {
    public static void main(String[] args) {

        // TODO 配置对象
        Map<String, Object> configMap = new HashMap<>();
        configMap.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, "hadoop104:9092");

        // TODO 创建管理者对象
        final Admin admin = Admin.create(configMap);

        // TODO 创建主题
        // 主题名称
        String topicName = "test1";
        // 分区梳理
        int partitionCount = 1;
        // 副本数量
        short replicationCount = 1;

        NewTopic topic1 = new NewTopic(topicName, partitionCount, replicationCount);

        String topicName1 = "test2";
        int partitionCount1 = 2;
        short replicationCount1 = 2;

        NewTopic topic2 = new NewTopic(topicName1, partitionCount1, replicationCount1);

        // TODO 自己分配副本方案
        String topicName2 = "test3";
        Map<Integer, List<Integer>> map = new HashMap<>();
        map.put(0, Arrays.asList(3,1));
        map.put(1, Arrays.asList(2,3));
        map.put(2, Arrays.asList(1,2));

        NewTopic topic3 = new NewTopic(topicName2, map);

        //topic1,
        final CreateTopicsResult topics = admin.createTopics(
                Arrays.asList(
                        topic2,topic3
                )
        );

        // TODO 关闭管理者对象
        admin.close();

    }
}
