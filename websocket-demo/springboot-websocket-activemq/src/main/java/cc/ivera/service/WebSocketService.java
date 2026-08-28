package cc.ivera.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.*;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class WebSocketService {

    private final SimpMessagingTemplate messagingTemplate;


    /**
     * 单独发送消息给某人
     * @param userId
     * @param message
     */
    @SneakyThrows
    public void send(String userId, String message) {
        String json = (new ObjectMapper()).writeValueAsString(new WebSocketResponseMessage(message));
        messagingTemplate.convertAndSendToUser(userId, "/topic/public", json);
    }

    /**
     * 群发消息给某人
     * @param message
     */
    @SneakyThrows
    public void send(String message) {
        messagingTemplate.convertAndSend( "/topic/public",message);
    }




    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class WebSocketResponseMessage {
        private String content;
    }

}
