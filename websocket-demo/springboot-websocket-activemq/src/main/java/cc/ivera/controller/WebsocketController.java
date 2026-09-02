package cc.ivera.controller;

import cc.ivera.entity.ChatMessage;
import cc.ivera.service.ChatService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

@Controller
public class WebsocketController {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebsocketController.class);

    @Value("${redis.set.onlineUsers}")
    private String onlineUsers;

    private final ChatService chatService;
    private final RedisTemplate<String, String> redisTemplate;

    public WebsocketController(ChatService chatService, RedisTemplate<String, String> redisTemplate) {
        this.chatService = chatService;
        this.redisTemplate = redisTemplate;
    }

    @ResponseBody
    @GetMapping("/getOnlineUsers")
    public Set<String> getOnlineUsers() {
        Set<String> resultSet = redisTemplate.opsForSet().members(onlineUsers);
        if (resultSet == null || resultSet.isEmpty()) {
            return Collections.emptySet();
        }
        return new TreeSet<>(resultSet);
    }

    /**
     * The only client message sending endpoint.
     * Frontend always sends to /app/chat.sendMessage.
     * to=all means group chat; otherwise the value is the target username and
     * the message is delivered through Spring's /user destination.
     */
    @MessageMapping("/chat.sendMessage")
    @SendToUser(destinations = ChatService.PRIVATE_DESTINATION, broadcast = false)
    public ChatMessage sendMessage(@Payload ChatMessage chatMessage, Principal principal) {
        try {
            if (chatMessage == null) {
                return null;
            }
            if (principal == null) {
                LOGGER.warn("Ignore websocket message because Principal is missing. Client sender={}, to={}",
                        chatMessage.getSender(), chatMessage.getTo());
                return null;
            }

            String content = chatMessage.getContent();
            if (content == null || content.trim().isEmpty()) {
                return null;
            }

            chatMessage.setSender(principal.getName());
            chatMessage.setContent(content.trim());
            chatMessage.setType(ChatMessage.MessageType.CHAT);
            if (chatMessage.getTo() == null || chatMessage.getTo().trim().isEmpty()) {
                chatMessage.setTo(ChatService.ALL_USERS);
            } else {
                chatMessage.setTo(chatMessage.getTo().trim());
            }

            chatService.sendMessage(chatMessage);

            // Group messages are already received by the sender from /topic/public.
            // For private messages return the server-confirmed payload so @SendToUser
            // echoes it directly to the WebSocket session that sent the message.
            return ChatService.ALL_USERS.equalsIgnoreCase(chatMessage.getTo()) ? null : chatMessage;
        } catch (Exception e) {
            LOGGER.error("Failed to send websocket message", e);
            return null;
        }
    }

}
