package cc.ivera.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import cc.ivera.entity.ChatMessage;
import cc.ivera.service.ChatService;
import cc.ivera.service.WebSocketService;
import cc.ivera.util.JsonUtil;
import cc.ivera.util.RedisUtils;

import java.util.Set;

@Controller
public class WebsocketController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketController.class);

    @Value("${redis.channel.msgToAll}")
    private String msgToAll;

    @Value("${redis.set.onlineUsers}")
    private String onlineUsers;

    @Value("${redis.channel.userStatus}")
    private String userStatus;

    @Value("${redis.channel.msgAlone}")
    private String msgAlone;

    @Autowired
    private ChatService chatService;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RedisUtils redisUtils;


    @Autowired
    private WebSocketService webSocketService;


    @ResponseBody
    @GetMapping("/getOnlineUsers")
    public void getOnlineUsersNum(){
        Set<String> resultSet = redisTemplate.opsForSet().members(onlineUsers);
        System.out.println("在线人数 :"+resultSet.size()+"\n");
        System.out.println("在线用户："+resultSet.toString());


    }
    @ResponseBody
    @GetMapping("/sendToOne")
    public void sendToOne(@RequestParam("uid") String uid,@RequestParam("content") String content ){
        ChatMessage chatMessage=new ChatMessage();
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        chatMessage.setContent(content);
        chatMessage.setTo(uid);
        chatMessage.setSender("系统消息");
        webSocketService.send(uid,JsonUtil.parseObjToJson(chatMessage));
    }

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage) {
        try {

            System.out.println("---------------群发消息----------");
            //   System.out.println(JsonUtil.parseObjToJson(chatMessage));
            //将消息使用redis进行推送，推送主题复用websocket中的主题 ‘websocket.msgToAll’
            //redisTemplate.convertAndSend(msgToAll, JsonUtil.parseObjToJson(chatMessage));
            webSocketService.send(JsonUtil.parseObjToJson(chatMessage));
            //  System.out.println("MSG277准备就绪！");
            //  redisTemplate.convertAndSend(msgAlone, JsonUtil.parseObjToJson(chatMessage));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    //发送消息给所有人
    @MessageMapping("/chat.sendMessageTest")
    public void sendMessageAlone(@Payload ChatMessage chatMessage) {
        try {
            System.out.println("---------------单发消息----------");
            System.out.println("模拟单对单发送消息！");
            chatMessage.setSender(chatMessage.getSender());
            //jmsMessagingTemplate.convertAndSend(this.topic, JsonUtil.parseObjToJson(chatMessage));
            webSocketService.send(JsonUtil.parseObjToJson(chatMessage));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }

    //用户上线通知放到公共队列
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage) {
        LOGGER.info("有用户加入到了websocket 消息室" + chatMessage.getSender());
        try {
            redisTemplate.opsForSet().add(onlineUsers, chatMessage.getSender());
            System.out.println(chatMessage.toString());
            webSocketService.send(JsonUtil.parseObjToJson(chatMessage));
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }
    }


    @ResponseBody
    @GetMapping("/sendToAll")
    public void sendToAll(@RequestParam("content") String content ){
        ChatMessage chatMessage=new ChatMessage();
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        chatMessage.setContent(content);
        chatMessage.setTo("all");
        chatMessage.setSender("系统消息");
        //rabbitTemplate.convertAndSend("topicWebSocketExchange","topic.public", JsonUtil.parseObjToJson(chatMessage));
        //jmsMessagingTemplate.convertAndSend(this.topic, JsonUtil.parseObjToJson(chatMessage));
        webSocketService.send(JsonUtil.parseObjToJson(chatMessage));
    }


}
