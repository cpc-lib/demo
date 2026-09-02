package cc.ivera.service;

import cc.ivera.entity.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

    public static final String ALL_USERS = "all";
    public static final String PUBLIC_DESTINATION = "/topic/public";
    public static final String PRIVATE_DESTINATION = "/queue/private";
    public static final String ONLINE_USERS_DESTINATION = "/topic/online-users";

    private final SimpMessageSendingOperations messagingTemplate;

    /**
     * Unified message routing:
     * 1. to=all -> public room topic
     * 2. to=username -> Spring /user destination (private message)
     */
    public void sendMessage(ChatMessage chatMessage) {
        if (chatMessage == null) {
            return;
        }

        if (ALL_USERS.equalsIgnoreCase(chatMessage.getTo())) {
            messagingTemplate.convertAndSend(PUBLIC_DESTINATION, chatMessage);
            return;
        }

        String targetUser = chatMessage.getTo();
        if (targetUser == null || targetUser.trim().isEmpty()) {
            return;
        }

        targetUser = targetUser.trim();
        LOGGER.info("Private websocket message: sender={}, target={}", chatMessage.getSender(), targetUser);
        messagingTemplate.convertAndSendToUser(targetUser, PRIVATE_DESTINATION, chatMessage);
    }

    public void broadcastOnlineUsers(Collection<String> users) {
        messagingTemplate.convertAndSend(ONLINE_USERS_DESTINATION, users);
    }
}
