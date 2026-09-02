package cc.ivera.config;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * Bind the userid sent in the STOMP CONNECT frame to Spring's Principal.
 * This is required by convertAndSendToUser(...), otherwise /user destinations
 * cannot resolve the target WebSocket session.
 */
@Component
public class WebSocketAuthInterceptor extends ChannelInterceptorAdapter {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = readHeaderAccessor(message);
        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String userId = readWebSocketIdHeader(accessor);
            accessor.setUser(new UserPrincipal(userId));
            accessor.setHeader("connection-time", LocalDateTime.now().toString());
        }
        return message;
    }

    private StompHeaderAccessor readHeaderAccessor(Message<?> message) {
        StompHeaderAccessor accessor = getAccessor(message);
        if (accessor == null) {
            throw new IllegalStateException("Unable to read STOMP headers");
        }
        return accessor;
    }

    private String readWebSocketIdHeader(StompHeaderAccessor accessor) {
        String userId = accessor.getFirstNativeHeader("userid");
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("STOMP CONNECT header 'userid' must not be empty");
        }
        return userId.trim();
    }

    StompHeaderAccessor getAccessor(Message<?> message) {
        return MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    }
}
