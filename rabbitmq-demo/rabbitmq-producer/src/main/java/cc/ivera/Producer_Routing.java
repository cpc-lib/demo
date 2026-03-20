package cc.ivera;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 路由模式
 */
public class Producer_Routing {

    public static void main(String[] args) throws IOException, TimeoutException {
        //1.获取连接工厂
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setHost("114.132.210.77");
        connectionFactory.setPort(5672);
        connectionFactory.setVirtualHost("/root");
        connectionFactory.setUsername("root");
        connectionFactory.setPassword("root");

        //2.获取connection
        Connection connection = connectionFactory.newConnection();

        //3.获取channel
        Channel channel = connection.createChannel();

        /**
         * DeclareOk exchangeDeclare(String exchange,
         *         BuiltinExchangeType type,
         *         boolean durable,
         *         boolean autoDelete,
         *         boolean internal,
         *         Map<String, Object> arguments)
         */

        //fanout-广播 发送消息到与之绑定队列
        //创建交换机(FANOUT("fanout"),DIRECT("direct"),TOPIC("topic"), HEADERS("headers");)

        String exchangeName = "direct";

        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.DIRECT, true, true, false, null);


        /**
         * queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete,
         *                                  Map<String, Object> arguments)
         */
        channel.queueDeclare("topic_queue", true, false, false, null);

        channel.queueDeclare("topic_info_queue", true, false, false, null);


        //绑定队列与交换机
        channel.queueBind("topic_queue", exchangeName, "route1");
        channel.queueBind("topic_info_queue", exchangeName, "route2");
        //交换机（fanout，direct topic）
        //消息内容

        String msg1 = "去你大爷的";


        String msg2 = "我丢";

        //发布订阅模式设置
        channel.basicPublish(exchangeName, "route1", null, msg1.getBytes());

        channel.basicPublish(exchangeName, "route2", null, msg2.getBytes());

        //关闭连接
        channel.close();

        connection.close();
    }
}
