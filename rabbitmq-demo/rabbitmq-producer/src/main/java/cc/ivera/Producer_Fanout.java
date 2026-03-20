package cc.ivera;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * fanout模式
 */
public class Producer_Fanout {

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

        String exchangeName="fanout";

        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.FANOUT,true,true,false,null);


        /**
         * queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete,
         *                                  Map<String, Object> arguments)
         */
        channel.queueDeclare("pubsub_1",true,false,false,null);

        channel.queueDeclare("pubsub_2",true,false,false,null);


        //绑定队列与交换机
        channel.queueBind("pubsub_1",exchangeName,"");
        channel.queueBind("pubsub_2",exchangeName,"");
        //交换机（fanout，direct topic）
        //消息内容
        String msg = "去你大爷的";


        //模式设置(设置routing可以无效)
        channel.basicPublish(exchangeName, "", null, msg.getBytes());


        //关闭连接
        channel.close();

        connection.close();
    }
}
