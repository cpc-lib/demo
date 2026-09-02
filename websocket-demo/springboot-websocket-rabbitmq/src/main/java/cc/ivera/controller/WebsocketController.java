package cc.ivera.controller;

import cc.ivera.entity.ChatMessage;
import cc.ivera.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

@Controller
public class WebsocketController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketController.class);
    private static final String RABBIT_EXCHANGE = "stomp";
    private static final String RABBIT_ROUTING_KEY = "topic.mine";

    @Value("${redis.set.onlineUsers}")
    private String onlineUsers;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    /**
     * Return users currently registered in the chat room.
     */
    @ResponseBody
    @GetMapping("/getOnlineUsers")
    public Set<String> getOnlineUsers() {
        Set<String> users = redisTemplate.opsForSet().members(onlineUsers);
        if (users == null || users.isEmpty()) {
            return Collections.emptySet();
        }
        return new TreeSet<>(users);
    }

    /**
     * Unified chat-message entry point.
     * to = "all"       -> group message
     * to = "username"  -> private message
     */
    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        try {
            if (principal == null) {
                throw new IllegalStateException("WebSocket user is not authenticated");
            }
            if (chatMessage == null || chatMessage.getContent() == null || chatMessage.getContent().trim().isEmpty()) {
                return;
            }

            chatMessage.setSender(principal.getName());
            chatMessage.setType(ChatMessage.MessageType.CHAT);
            if (chatMessage.getTo() == null || chatMessage.getTo().trim().isEmpty()) {
                chatMessage.setTo("all");
            } else {
                chatMessage.setTo(chatMessage.getTo().trim());
            }

            LOGGER.info("send chat message: sender={}, to={}", chatMessage.getSender(), chatMessage.getTo());
            rabbitTemplate.convertAndSend(RABBIT_EXCHANGE, RABBIT_ROUTING_KEY, JsonUtil.parseObjToJson(chatMessage));
        } catch (Exception e) {
            LOGGER.error("Failed to send chat message", e);
        }
    }

    /**
     * Register a connected user and broadcast the JOIN event.
     */
    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessage chatMessage, Principal principal) {
        try {
            if (principal == null) {
                throw new IllegalStateException("WebSocket user is not authenticated");
            }

            String username = principal.getName();
            redisTemplate.opsForSet().add(onlineUsers, username);

            ChatMessage joinMessage = new ChatMessage();
            joinMessage.setSender(username);
            joinMessage.setType(ChatMessage.MessageType.JOIN);
            joinMessage.setTo("all");
            joinMessage.setContent("");

            LOGGER.info("user joined chat room: {}", username);
            rabbitTemplate.convertAndSend(RABBIT_EXCHANGE, RABBIT_ROUTING_KEY, JsonUtil.parseObjToJson(joinMessage));
        } catch (Exception e) {
            LOGGER.error("Failed to register WebSocket user", e);
        }
    }
}
