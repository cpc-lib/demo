package cc.ivera;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * 工作队列模式
 */
public class Producer_WorkQueue {

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
         * queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete,
         *                                  Map<String, Object> arguments)
         */


        channel.queueDeclare("work_queue",true,false,false,null);

        for(int i=1;i<=9;i++){
            //消息内容
            String msg = i+"-->Hello world!";

            //work_queue提高处理速度，consumer之间为竞争者关系(需要配置routingkey与queue相同)
            channel.basicPublish("", "work_queue", null, msg.getBytes());
        }


        //关闭连接
        channel.close();

        connection.close();
    }
}
