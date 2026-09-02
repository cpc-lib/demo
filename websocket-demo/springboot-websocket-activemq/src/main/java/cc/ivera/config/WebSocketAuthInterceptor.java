package cc.ivera.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Binds the logical chat username from the STOMP CONNECT frame to Spring's
 * WebSocket Principal. Spring's /user destination resolution relies on this
 * Principal name to find the target user's active WebSocket sessions.
 */
@Component
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSocketAuthInterceptor.class);
    private static final String USER_ID_HEADER = "userid";

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();
        if (StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) {
            // IMPORTANT: do not cast native header values to LinkedList.
            // Spring exposes native headers as List<String> and commonly stores
            // them as ArrayList. getFirstNativeHeader() is the supported API.
            String userId = accessor.getFirstNativeHeader(USER_ID_HEADER);
            if (StringUtils.hasText(userId)) {
                String username = userId.trim();
                accessor.setUser(new UserPrincipal(username));
                LOGGER.info("WebSocket user authenticated: username={}, sessionId={}",
                        username, accessor.getSessionId());
            } else {
                LOGGER.warn("WebSocket CONNECT rejected from user routing: missing '{}' header, sessionId={}",
                        USER_ID_HEADER, accessor.getSessionId());
            }
        }

        return message;
    }
}
