package cc.ivera.config;

import cc.ivera.entity.ChatMessage;
import cc.ivera.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;

/**
 * Keep the Redis online-user set in sync with WebSocket disconnects.
 */
@Component
public class WebSocketEventListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketEventListener.class);
    private static final String RABBIT_EXCHANGE = "stomp";
    private static final String RABBIT_ROUTING_KEY = "topic.mine";

    @Value("${redis.set.onlineUsers}")
    private String onlineUsers;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @EventListener
    public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
        Principal principal = event.getUser();
        if (principal == null || principal.getName() == null || principal.getName().trim().isEmpty()) {
            return;
        }

        String username = principal.getName().trim();
        Long removed = redisTemplate.opsForSet().remove(onlineUsers, username);
        if (removed == null || removed <= 0) {
            return;
        }

        ChatMessage leaveMessage = new ChatMessage();
        leaveMessage.setSender(username);
        leaveMessage.setType(ChatMessage.MessageType.LEAVE);
        leaveMessage.setTo("all");
        leaveMessage.setContent("");

        LOGGER.info("user left chat room: {}", username);
        rabbitTemplate.convertAndSend(RABBIT_EXCHANGE, RABBIT_ROUTING_KEY, JsonUtil.parseObjToJson(leaveMessage));
    }
}
