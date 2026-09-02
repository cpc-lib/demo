package cc.ivera.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RefundSubmitRabbitConfig {

    public static final String REFUND_SUBMIT_EVENT_EXCHANGE = "payment.refund.submit.event.exchange";
    public static final String REFUND_SUBMIT_RETRY_EXCHANGE = "payment.refund.submit.retry.exchange";
    public static final String REFUND_SUBMIT_QUEUE = "payment.refund.submit.queue";
    public static final String REFUND_SUBMIT_RETRY_QUEUE = "payment.refund.submit.retry.queue";
    public static final String REFUND_SUBMIT_ROUTING_KEY = "payment.refund.submit";
    public static final String REFUND_SUBMIT_RETRY_ROUTING_KEY = "payment.refund.submit.retry";

    @Bean(name = "refundSubmitEventExchange")
    public DirectExchange refundSubmitEventExchange() {
        return new DirectExchange(REFUND_SUBMIT_EVENT_EXCHANGE, true, false);
    }

    @Bean(name = "refundSubmitRetryExchange")
    public DirectExchange refundSubmitRetryExchange() {
        return new DirectExchange(REFUND_SUBMIT_RETRY_EXCHANGE, true, false);
    }

    @Bean(name = "refundSubmitQueue")
    public Queue refundSubmitQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", REFUND_SUBMIT_RETRY_EXCHANGE);
        args.put("x-dead-letter-routing-key", REFUND_SUBMIT_RETRY_ROUTING_KEY);
        return new Queue(REFUND_SUBMIT_QUEUE, true, false, false, args);
    }

    @Bean(name = "refundSubmitRetryQueue")
    public Queue refundSubmitRetryQueue(
            @Value("${payment.refund.submit-retry-ms:10000}") long retryDelayMs
    ) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-message-ttl", Math.max(1000L, retryDelayMs));
        args.put("x-dead-letter-exchange", REFUND_SUBMIT_EVENT_EXCHANGE);
        args.put("x-dead-letter-routing-key", REFUND_SUBMIT_ROUTING_KEY);
        return new Queue(REFUND_SUBMIT_RETRY_QUEUE, true, false, false, args);
    }

    @Bean
    public Binding refundSubmitBinding(
            @Qualifier("refundSubmitQueue") Queue queue,
            @Qualifier("refundSubmitEventExchange") DirectExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with(REFUND_SUBMIT_ROUTING_KEY);
    }

    @Bean
    public Binding refundSubmitRetryBinding(
            @Qualifier("refundSubmitRetryQueue") Queue queue,
            @Qualifier("refundSubmitRetryExchange") DirectExchange exchange
    ) {
        return BindingBuilder.bind(queue).to(exchange).with(REFUND_SUBMIT_RETRY_ROUTING_KEY);
    }
}
