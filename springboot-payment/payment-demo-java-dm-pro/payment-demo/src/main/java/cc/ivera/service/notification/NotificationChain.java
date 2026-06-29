package cc.ivera.service.notification;

import java.util.List;

/**
 * 通知处理责任链 — Chain of Responsibility 模式。
 *
 * 按顺序执行注册的 Handler，任一节点失败则中断链路。
 */
public class NotificationChain {

    private final List<NotificationHandler> handlers;
    private int currentIndex = 0;

    public NotificationChain(List<NotificationHandler> handlers) {
        this.handlers = handlers;
    }

    /**
     * 启动责任链处理。
     */
    public void proceed(NotificationContext context) {
        if (currentIndex < handlers.size()) {
            NotificationHandler handler = handlers.get(currentIndex++);
            handler.handle(context, this);
        }
    }
}
