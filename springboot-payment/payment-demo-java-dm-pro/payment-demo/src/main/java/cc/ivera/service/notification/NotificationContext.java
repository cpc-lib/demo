package cc.ivera.service.notification;

import java.util.Map;

/**
 * 通知处理上下文 — 在责任链各节点间传递数据。
 */
public class NotificationContext {

    /** 原始通知 body */
    private final String rawBody;

    /** 解析后的通知参数 */
    private final Map<String, Object> bodyMap;

    /** 通知 ID（幂等 key） */
    private String notifyId;

    /** 订单号 */
    private String orderNo;

    /** 处理结果：是否已处理 */
    private boolean processed;

    /** 错误信息 */
    private String errorMessage;

    public NotificationContext(String rawBody, Map<String, Object> bodyMap) {
        this.rawBody = rawBody;
        this.bodyMap = bodyMap;
    }

    public String getRawBody() { return rawBody; }
    public Map<String, Object> getBodyMap() { return bodyMap; }
    public String getNotifyId() { return notifyId; }
    public void setNotifyId(String notifyId) { this.notifyId = notifyId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public boolean isProcessed() { return processed; }
    public void setProcessed(boolean processed) { this.processed = processed; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
