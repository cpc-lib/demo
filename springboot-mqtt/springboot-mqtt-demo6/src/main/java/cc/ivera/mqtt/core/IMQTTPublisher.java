package cc.ivera.mqtt.core;

/**
 * <p>
 * 发布者接口
 */
public interface IMQTTPublisher {
    /**
     * 发布消息
     *
     * @param topic   主题
     * @param message 消息
     */
    public void publishMessage(String topic, String message);

    /**
     * 断开MQTT客户端
     */
    public void disconnect();
}

