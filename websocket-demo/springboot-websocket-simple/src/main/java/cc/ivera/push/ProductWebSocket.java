package cc.ivera.push;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicInteger;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;

import cc.ivera.config.EndpointConfigure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * @Author：JCccc
 * @Description：
 * @Date： created in 15:56 2019/5/13
 */

@Component
@ServerEndpoint(value = "/productWebSocket/{userId}",
        configurator = EndpointConfigure.class)
public class ProductWebSocket {

    // 静态变量，用来记录当前在线连接数。应该把它设计成线程安全的。
    private static final AtomicInteger OnlineCount = new AtomicInteger(0);

    // concurrent包的线程安全Set，用来存放所有客户端的连接会话。
    // 注意：EndpointConfigure返回的是Spring单例，所有连接共享同一个ProductWebSocket实例，
    // 因此不能把session、userId存为成员变量（会被连接间互相覆盖），需绑定到各自的Session上。
    private static CopyOnWriteArraySet<Session> webSocketSet = new CopyOnWriteArraySet<Session>();

    private static final Logger log = LoggerFactory.getLogger(ProductWebSocket.class);

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(@PathParam("userId") String userId, Session session) {
        log.info("新客户端连入，用户id：" + userId);
        // 将userId绑定到该连接自己的session上
        session.getUserProperties().put("userId", userId);
        webSocketSet.add(session); // 加入set中
        addOnlineCount(); // 在线数加1
        if (userId != null) {
            sendMessage(session, userId + "连接成功-" + "-当前在线人数为：" + getOnlineCount());
        }
    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(Session session) {
        log.info("一个客户端关闭连接");
        webSocketSet.remove(session); // 从set中删除
        subOnlineCount(); // 在线数减1
    }

    /**
     * 收到客户端消息后调用的方法，将消息转发给所有在线用户
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, Session session) throws IOException {
        String userId = (String) session.getUserProperties().get("userId");
        log.info("用户" + userId + "发送过来的消息为：" + message);
        sendInfo(userId + ": " + message);
    }

    /**
     * 发生错误时调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("websocket出现错误");
        error.printStackTrace();
    }

    /**
     * 向指定客户端发送消息
     */
    public static void sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
            log.info("推送消息成功，消息为：" + message);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 群发自定义消息
     */
    public static void sendInfo(String message) throws IOException {
        for (Session session : webSocketSet) {
            sendMessage(session, message);
        }
    }

    public static synchronized int getOnlineCount() {
        return OnlineCount.get();
    }

    public static synchronized void addOnlineCount() {
        OnlineCount.incrementAndGet(); // 在线数加1
    }

    public static synchronized void subOnlineCount() {
        OnlineCount.decrementAndGet(); // 在线数减1
    }
}
