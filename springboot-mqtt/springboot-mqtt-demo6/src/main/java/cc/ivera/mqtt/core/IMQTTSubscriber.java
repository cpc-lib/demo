package cc.ivera.mqtt.core;

/**
 * <p>
 * 订阅者接口
 */
public interface IMQTTSubscriber {

    /**
     * 订阅消息
     *
     * @param topic
     */
    public void subscribeMessage(String topic);

    /**
     * 断开MQTT客户端
     */
    public void disconnect();
}
