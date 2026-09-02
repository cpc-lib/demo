package cc.ivera.service;

import cc.ivera.entity.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class WebSocketPresenceListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketPresenceListener.class);

    @Value("${redis.set.onlineUsers}")
    private String onlineUsersKey;

    private final RedisTemplate<String, String> redisTemplate;
    private final ChatService chatService;

    /** sessionId -> username. Used to support multiple browser tabs for one user. */
    private final ConcurrentMap<String, String> sessionUsers = new ConcurrentHashMap<>();

    public WebSocketPresenceListener(RedisTemplate<String, String> redisTemplate, ChatService chatService) {
        this.redisTemplate = redisTemplate;
        this.chatService = chatService;
    }

    @EventListener
    public void handleConnected(SessionConnectedEvent event) {
        Principal principal = event.getUser();
        String sessionId = (String) event.getMessage().getHeaders().get("simpSessionId");
        if (principal == null || sessionId == null) {
            return;
        }

        String username = principal.getName();
        sessionUsers.put(sessionId, username);

        Long added = redisTemplate.opsForSet().add(onlineUsersKey, username);
        if (added != null && added > 0) {
            LOGGER.info("用户进入消息室: {}", username);
            chatService.sendMessage(presenceMessage(ChatMessage.MessageType.JOIN, username));
        }
        broadcastOnlineUsers();
    }

    @EventListener
    public void handleDisconnected(SessionDisconnectEvent event) {
        String sessionId = event.getSessionId();
        String username = sessionUsers.remove(sessionId);
        if (username == null && event.getUser() != null) {
            username = event.getUser().getName();
        }
        if (username == null || username.trim().isEmpty()) {
            return;
        }

        // Keep the user online while another tab/session for the same username exists.
        if (sessionUsers.containsValue(username)) {
            broadcastOnlineUsers();
            return;
        }

        Long removed = redisTemplate.opsForSet().remove(onlineUsersKey, username);
        if (removed != null && removed > 0) {
            LOGGER.info("用户离开消息室: {}", username);
            chatService.sendMessage(presenceMessage(ChatMessage.MessageType.LEAVE, username));
        }
        broadcastOnlineUsers();
    }

    private ChatMessage presenceMessage(ChatMessage.MessageType type, String username) {
        ChatMessage message = new ChatMessage();
        message.setType(type);
        message.setSender(username);
        message.setTo(ChatService.ALL_USERS);
        return message;
    }

    private void broadcastOnlineUsers() {
        Set<String> users = redisTemplate.opsForSet().members(onlineUsersKey);
        if (users == null || users.isEmpty()) {
            chatService.broadcastOnlineUsers(Collections.<String>emptySet());
            return;
        }
        chatService.broadcastOnlineUsers(new TreeSet<>(users));
    }
}
