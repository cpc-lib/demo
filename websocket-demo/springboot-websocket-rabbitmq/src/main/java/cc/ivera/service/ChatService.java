package cc.ivera.service;

import cc.ivera.entity.ChatMessage;
import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);
    private static final String PUBLIC_DESTINATION = "/topic/public";
    private static final String PRIVATE_DESTINATION = "/topic/msg";

    @Autowired
    private SimpMessageSendingOperations simpMessageSendingOperations;

    /**
     * Route a RabbitMQ message to the appropriate WebSocket destination.
     *
     * Group/JOIN/LEAVE -> /topic/public
     * Private CHAT     -> /user/{username}/topic/msg
     *
     * For private chat the sender also receives a copy so the message is
     * visible immediately in both participants' chat windows.
     */
    public Boolean sendMsg(String msg) {
        try {
            JSONObject msgJson = JSONObject.parseObject(msg);
            String to = msgJson.getString("to");
            String sender = msgJson.getString("sender");
            String type = msgJson.getString("type");

            if (type == null) {
                LOGGER.warn("Ignore message without type: {}", msg);
                return true;
            }

            if (ChatMessage.MessageType.JOIN.toString().equals(type)
                    || ChatMessage.MessageType.LEAVE.toString().equals(type)
                    || "all".equalsIgnoreCase(to)) {
                simpMessageSendingOperations.convertAndSend(PUBLIC_DESTINATION, msgJson);
                return true;
            }

            if (ChatMessage.MessageType.CHAT.toString().equals(type) && to != null && !to.trim().isEmpty()) {
                String targetUser = to.trim();
                simpMessageSendingOperations.convertAndSendToUser(targetUser, PRIVATE_DESTINATION, msgJson);

                // Echo a private message back to the sender as well. Without this,
                // lisi -> zhangsan is visible only to zhangsan, not to lisi.
                if (sender != null && !sender.trim().isEmpty() && !targetUser.equals(sender.trim())) {
                    simpMessageSendingOperations.convertAndSendToUser(sender.trim(), PRIVATE_DESTINATION, msgJson);
                }
                return true;
            }

            LOGGER.warn("Ignore unsupported chat message: {}", msg);
            return true;
        } catch (RuntimeException e) {
            LOGGER.error("Failed to push RabbitMQ message to WebSocket: {}", msg, e);
            return false;
        }
    }
}
