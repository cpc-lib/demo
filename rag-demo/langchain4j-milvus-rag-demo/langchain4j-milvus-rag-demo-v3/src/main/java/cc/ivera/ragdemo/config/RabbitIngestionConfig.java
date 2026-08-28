package cc.ivera.ragdemo.config;


import lombok.RequiredArgsConstructor;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RabbitIngestionConfig {

    private final RagProperties properties;

    @Bean
    public DirectExchange ragIngestionExchange() {
        return new DirectExchange(properties.getIngestion().getExchange(), true, false);
    }

    @Bean
    public Queue ragIngestionQueue() {
        return new Queue(properties.getIngestion().getQueueName(), true);
    }

    @Bean
    public Binding ragIngestionBinding(Queue ragIngestionQueue, DirectExchange ragIngestionExchange) {
        return BindingBuilder
                .bind(ragIngestionQueue)
                .to(ragIngestionExchange)
                .with(properties.getIngestion().getRoutingKey());
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
