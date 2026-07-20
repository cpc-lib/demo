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
public class ReconciliationRabbitConfig {

    public static final String RECONCILIATION_EXECUTE_EXCHANGE = "payment.reconciliation.execute.event.exchange";
    public static final String RECONCILIATION_EXECUTE_QUEUE = "payment.reconciliation.execute.queue";
    public static final String RECONCILIATION_EXECUTE_ROUTING_KEY = "payment.reconciliation.execute";
    public static final String RECONCILIATION_RETRY_DEAD_LETTER_EXCHANGE = "payment.reconciliation.retry.dead-letter.exchange";
    public static final String RECONCILIATION_RETRY_DELAY_QUEUE = "payment.reconciliation.retry.delay.queue";
    public static final String RECONCILIATION_RETRY_RELEASE_QUEUE = "payment.reconciliation.retry.release.queue";
    public static final String RECONCILIATION_RETRY_DELAY_ROUTING_KEY = "payment.reconciliation.retry.delay";
    public static final String RECONCILIATION_RETRY_RELEASE_ROUTING_KEY = "payment.reconciliation.retry.release";

    @Bean(name = "reconciliationExecuteExchange")
    public DirectExchange reconciliationExecuteExchange() {
        return new DirectExchange(RECONCILIATION_EXECUTE_EXCHANGE, true, false);
    }

    @Bean(name = "reconciliationRetryDeadLetterExchange")
    public DirectExchange reconciliationRetryDeadLetterExchange() {
        return new DirectExchange(RECONCILIATION_RETRY_DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean(name = "reconciliationExecuteQueue")
    public Queue reconciliationExecuteQueue() {
        return new Queue(RECONCILIATION_EXECUTE_QUEUE, true);
    }

    @Bean(name = "reconciliationRetryDelayQueue")
    public Queue reconciliationRetryDelayQueue(@Value("${payment.reconciliation.retry-delay-ms:300000}") long retryDelayMs) {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", RECONCILIATION_RETRY_DEAD_LETTER_EXCHANGE);
        args.put("x-dead-letter-routing-key", RECONCILIATION_RETRY_RELEASE_ROUTING_KEY);
        args.put("x-message-ttl", retryDelayMs);
        return new Queue(RECONCILIATION_RETRY_DELAY_QUEUE, true, false, false, args);
    }

    @Bean(name = "reconciliationRetryReleaseQueue")
    public Queue reconciliationRetryReleaseQueue() {
        return new Queue(RECONCILIATION_RETRY_RELEASE_QUEUE, true);
    }

    @Bean
    public Binding reconciliationExecuteBinding(
            @Qualifier("reconciliationExecuteQueue") Queue reconciliationExecuteQueue,
            @Qualifier("reconciliationExecuteExchange") DirectExchange reconciliationExecuteExchange) {
        return BindingBuilder.bind(reconciliationExecuteQueue)
                .to(reconciliationExecuteExchange)
                .with(RECONCILIATION_EXECUTE_ROUTING_KEY);
    }

    @Bean
    public Binding reconciliationRetryDelayBinding(
            @Qualifier("reconciliationRetryDelayQueue") Queue reconciliationRetryDelayQueue,
            @Qualifier("reconciliationExecuteExchange") DirectExchange reconciliationExecuteExchange) {
        return BindingBuilder.bind(reconciliationRetryDelayQueue)
                .to(reconciliationExecuteExchange)
                .with(RECONCILIATION_RETRY_DELAY_ROUTING_KEY);
    }

    @Bean
    public Binding reconciliationRetryReleaseBinding(
            @Qualifier("reconciliationRetryReleaseQueue") Queue reconciliationRetryReleaseQueue,
            @Qualifier("reconciliationRetryDeadLetterExchange") DirectExchange reconciliationRetryDeadLetterExchange) {
        return BindingBuilder.bind(reconciliationRetryReleaseQueue)
                .to(reconciliationRetryDeadLetterExchange)
                .with(RECONCILIATION_RETRY_RELEASE_ROUTING_KEY);
    }
}
