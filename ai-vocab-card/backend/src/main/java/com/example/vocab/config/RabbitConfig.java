package com.example.vocab.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String EXCHANGE = "vocab.exchange";
    public static final String DOMAIN_EXCHANGE = "ai.vocab.domain.exchange";
    public static final String ANKI_EXPORT_QUEUE = "vocab.anki.export.queue";
    public static final String ANKI_EXPORT_ROUTING_KEY = "vocab.anki.export";

    @Bean
    DirectExchange vocabExchange() { return new DirectExchange(EXCHANGE, true, false); }

    @Bean
    TopicExchange domainExchange() { return new TopicExchange(DOMAIN_EXCHANGE, true, false); }

    @Bean
    Queue ankiExportQueue() { return QueueBuilder.durable(ANKI_EXPORT_QUEUE).build(); }

    @Bean
    Binding ankiExportBinding() { return BindingBuilder.bind(ankiExportQueue()).to(vocabExchange()).with(ANKI_EXPORT_ROUTING_KEY); }

    @Bean
    MessageConverter messageConverter() { return new Jackson2JsonMessageConverter(); }
}
