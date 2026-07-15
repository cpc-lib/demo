package cc.ivera.websocket;

import cc.ivera.model.SocketMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class TemperatureWebSocketHandler extends TextWebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(TemperatureWebSocketHandler.class);
    private final Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    private final ObjectMapper objectMapper;

    public TemperatureWebSocketHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws IOException {
        sessions.add(session);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(
                SocketMessage.of("connection", java.util.Map.of("status", "connected")))));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
    }

    public void broadcast(String type, Object payload) {
        try {
            TextMessage message = new TextMessage(objectMapper.writeValueAsString(SocketMessage.of(type, payload)));
            sessions.removeIf(session -> !sendSafely(session, message));
        } catch (Exception ex) {
            log.error("Unable to serialize WebSocket message", ex);
        }
    }

    private boolean sendSafely(WebSocketSession session, TextMessage message) {
        if (!session.isOpen()) return false;
        try {
            synchronized (session) {
                session.sendMessage(message);
            }
            return true;
        } catch (IOException ex) {
            log.warn("Removing unavailable WebSocket session {}", session.getId());
            return false;
        }
    }

    public int connectionCount() {
        return sessions.size();
    }
}
