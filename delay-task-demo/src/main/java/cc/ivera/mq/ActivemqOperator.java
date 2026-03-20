package cc.ivera.mq;

import org.apache.activemq.ScheduledMessage;
import org.apache.activemq.command.ActiveMQQueue;
import org.apache.activemq.command.ActiveMQTopic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jms.JmsProperties;
import org.springframework.jms.core.JmsMessagingTemplate;
import org.springframework.stereotype.Component;

import javax.jms.*;


@Component
public class ActivemqOperator {
    private final JmsMessagingTemplate jmsTemplate;
    @Autowired
    public ActivemqOperator(JmsMessagingTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    /**
     * 发送主题消息
     */
    public void sendMsgTopic(String topic, String message) {
        Destination destination = new ActiveMQTopic(topic);
        jmsTemplate.convertAndSend(destination, message);
    }

    /**
     * 发送队列消息
     */
    public void sendMsgQueue(String queue, String message) {
        Destination destination = new ActiveMQQueue(queue);
        jmsTemplate.convertAndSend(destination, message);
    }

    /**
     * 发送延时队列消息
     */
    public <T> void delaySend(String queue, String message, Long time) {
        Connection connection = null;
        Session session = null;
        MessageProducer producer = null;
        ConnectionFactory connectionFactory = jmsTemplate.getConnectionFactory();
        Destination destination = new ActiveMQQueue(queue);
        try {
            connection = connectionFactory.createConnection();
            connection.start();
            session = connection.createSession(Boolean.TRUE, Session.AUTO_ACKNOWLEDGE);
            producer = session.createProducer(destination);
            producer.setDeliveryMode(JmsProperties.DeliveryMode.PERSISTENT.getValue());
            ObjectMessage objectMessage = session.createObjectMessage(message);

            objectMessage.setLongProperty(ScheduledMessage.AMQ_SCHEDULED_DELAY, time);

            producer.send(objectMessage);
            session.commit();
        } catch (Exception e) {
            throw new RuntimeException("delaySend exception : " + e.getMessage());
        } finally {
            try {
                if (producer != null) {
                    producer.close();
                }
                if (session != null) {
                    session.close();
                }
                if (connection != null) {
                    connection.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

