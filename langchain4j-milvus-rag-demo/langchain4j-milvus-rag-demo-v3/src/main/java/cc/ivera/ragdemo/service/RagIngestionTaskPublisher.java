package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.model.knowledge.RagIngestionTaskMessage;
import cc.ivera.ragdemo.util.TraceUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RagIngestionTaskPublisher {

    private final RagProperties properties;
    private final RabbitTemplate rabbitTemplate;

    public boolean publish(RagIngestionTaskMessage message) {
        if (!properties.getIngestion().isRabbitEnabled()) {
            return false;
        }
        rabbitTemplate.convertAndSend(
                properties.getIngestion().getExchange(),
                properties.getIngestion().getRoutingKey(),
                message,
                amqpMessage -> {
                    if (message.tenantId() != null) {
                        amqpMessage.getMessageProperties().setHeader("tenantId", message.tenantId());
                    }
                    if (message.knowledgeBaseId() != null) {
                        amqpMessage.getMessageProperties().setHeader("knowledgeBaseId", message.knowledgeBaseId());
                    }
                    if (message.traceId() != null) {
                        amqpMessage.getMessageProperties().setHeader("traceId", message.traceId());
                    }
                    return amqpMessage;
                }
        );
        return true;
    }

    public boolean publishWithCurrentTrace(RagIngestionTaskMessage message) {
        return publish(new RagIngestionTaskMessage(
                message.tenantId(),
                message.taskId(),
                message.documentId(),
                message.knowledgeBaseId(),
                message.documentVersionId(),
                message.taskNo(),
                TraceUtils.currentTraceId()
        ));
    }
}
