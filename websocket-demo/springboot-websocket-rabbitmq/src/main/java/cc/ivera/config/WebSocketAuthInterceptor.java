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
 * @Author : JCccc
 * @CreateTime : 2020/8/26
 * @Description :
 **/
@Component
public class WebSocketAuthInterceptor extends ChannelInterceptorAdapter {

//    @Override
//    public Message<?> preSend(Message<?> message, MessageChannel channel) {
//        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
//        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
//            Object raw = message.getHeaders().get(SimpMessageHeaderAccessor.NATIVE_HEADERS);
//            if (raw instanceof Map) {
//                Object name = ((Map) raw).get("userid");
//                System.out.println(name);
//                if (name instanceof LinkedList) {
//                    // 设置当前访问的认证用户
//                    accessor.setUser(new UserPrincipal(((LinkedList) name).get(0).toString()));
//                }
//            }
//        }
//        return message;
//    }


    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        final StompHeaderAccessor accessor = readHeaderAccessor(message);
        if (accessor.getCommand() == StompCommand.CONNECT) {
            String wsId = readWebSocketIdHeader(accessor);
            accessor.setHeader("connection-time", LocalDateTime.now().toString());
        }
        return message;
    }

    /**
     * Instantiate an object for retrieving the STOMP headers
     */
    private StompHeaderAccessor readHeaderAccessor(Message<?> message) {
        final StompHeaderAccessor accessor = getAccessor(message);
        if (accessor == null) {
            throw new RuntimeException();
        }
        return accessor;
    }

    private String readWebSocketIdHeader(StompHeaderAccessor accessor) {
        final String userId = accessor.getFirstNativeHeader("userid");
        if (userId == null || userId.trim().isEmpty()) {
            throw new RuntimeException();
        }
        return userId;
    }

    StompHeaderAccessor getAccessor(Message<?> message) {
        return MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
    }
}