package cc.ivera;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.IdcardUtil;
import com.alibaba.fastjson2.JSON;
import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.sun.corba.se.impl.presentation.rmi.IDLTypesUtil;
import com.sun.org.apache.xml.internal.utils.StringComparable;
import com.sun.xml.internal.bind.v2.schemagen.xmlschema.List;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

/**
 * Topic模式
 * 通配符 *.代表一个
 * #,0个或任意个
 */
public class Producer_Topic {
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

        String exchangeName = "topic";

        channel.exchangeDeclare(exchangeName, BuiltinExchangeType.TOPIC, true, true, false, null);


        /**
         * queueDeclare(String queue, boolean durable, boolean exclusive, boolean autoDelete,
         *                                  Map<String, Object> arguments)
         */
        channel.queueDeclare("music:chinese:queue", true, false, false, null);
        channel.queueDeclare("movie:chinese:queue", true, false, false, null);
        channel.queueDeclare("music:usa:queue", true, false, false, null);
        channel.queueDeclare("movie:usa:queue", true, false, false, null);
        channel.queueDeclare("music:queue", true, false, false, null);
        channel.queueDeclare("movie:queue", true, false, false, null);
        channel.queueDeclare("chinese:queue", true, false, false, null);
        channel.queueDeclare("usa:queue", true, false, false, null);


        //绑定队列与交换机
        channel.queueBind("music:chinese:queue", exchangeName, "music.chinese");
        channel.queueBind("movie:chinese:queue", exchangeName, "movie.chinese");
        channel.queueBind("chinese:queue", exchangeName, "#.chinese");
        channel.queueBind("usa:queue", exchangeName, "#.usa");
        channel.queueBind("music:usa:queue", exchangeName, "music.usa");
        channel.queueBind("movie:usa:queue",exchangeName,"movie:usa");
        channel.queueBind("music:queue", exchangeName, "music.#");
        channel.queueBind("movie:queue", exchangeName, "movie.#");

        //交换机（fanout，direct topic）
        //消息内容


        ArrayList<String> usaMovieList = new ArrayList<String>();
        usaMovieList.add("美女与野兽");
        usaMovieList.add("终局之战");


        ArrayList<String> usaMusicList= new ArrayList<String>();
        usaMusicList.add("try");
        usaMusicList.add("Imagination");

        ArrayList<String> chineseMovieList = new ArrayList<String>();
        chineseMovieList.add("飞驰人生");
        chineseMovieList.add("西虹市首富");

        ArrayList<String> chineseMusicList= new ArrayList<String>();
        chineseMusicList.add("国王与骑士");
        chineseMusicList.add("Falling in you");



        Map<String,Object> chineseMusicMap=new HashMap<String,Object>();
        chineseMusicMap.put("id", IdUtil.getSnowflake().nextId());
        chineseMusicMap.put("tag","chinese-music");
        chineseMusicMap.put("data",chineseMusicList);


        Map<String,Object> chineseMovieMap=new HashMap<String,Object>();
        chineseMovieMap.put("id", IdUtil.getSnowflake().nextId());
        chineseMovieMap.put("tag","chinese-movie");
        chineseMovieMap.put("data",chineseMovieList);


        String chineseMovieStr = JSON.toJSONString(chineseMovieMap);
        String chineseMusicStr = JSON.toJSONString(chineseMusicMap);





        channel.basicPublish(exchangeName, "movie.chinese", null, chineseMovieStr.getBytes());


        channel.basicPublish(exchangeName, "music.chinese", null, chineseMusicStr.getBytes());


//        channel.basicPublish(exchangeName, "movie.usa", null, usaMovieList.toString().getBytes());
//
//
//        channel.basicPublish(exchangeName, "music.usa", null,usaMusicList.toString().getBytes());

        //关闭连接
        channel.close();

        connection.close();
    }
}
