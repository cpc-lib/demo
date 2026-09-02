package cc.ivera.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.messaging.simp.SimpMessageSendingOperations;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;

class ChatServiceTest {

    @Mock
    private SimpMessageSendingOperations messaging;

    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void shouldRouteGroupMessageToPublicTopic() {
        String msg = "{\"type\":\"CHAT\",\"sender\":\"lisi\",\"to\":\"all\",\"content\":\"hello\"}";

        chatService.sendMsg(msg);

        verify(messaging).convertAndSend(eq("/topic/public"), any(Object.class));
        verify(messaging, never()).convertAndSendToUser(any(String.class), any(String.class), any());
    }

    @Test
    void shouldRoutePrivateMessageToRecipientAndSender() {
        String msg = "{\"type\":\"CHAT\",\"sender\":\"lisi\",\"to\":\"zhangsan\",\"content\":\"hello\"}";

        chatService.sendMsg(msg);

        verify(messaging).convertAndSendToUser(eq("zhangsan"), eq("/topic/msg"), any());
        verify(messaging).convertAndSendToUser(eq("lisi"), eq("/topic/msg"), any());
    }

    @Test
    void shouldNotDuplicatePrivateMessageWhenSendingToSelf() {
        String msg = "{\"type\":\"CHAT\",\"sender\":\"lisi\",\"to\":\"lisi\",\"content\":\"hello\"}";

        chatService.sendMsg(msg);

        verify(messaging, times(1)).convertAndSendToUser(eq("lisi"), eq("/topic/msg"), any());
    }
}
