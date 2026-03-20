package cc.ivera.topic.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TopicRabbitConfig {

	public final static String QUEUE_NAME = "spring-boot-queue-2";
	public final static String EXCHANGE_NAME = "spring-boot-exchange-2";
	public final static String ROUTING_KEY = "spring-boot-key-2";


	@Bean("queueMessage")
	public Queue queueMessage() {
		return new Queue(QUEUE_NAME);
	}




	@Bean("exchange")
	TopicExchange exchange() {
		return new TopicExchange(EXCHANGE_NAME);
	}


	@Bean
	Binding bindingExchangeMessage(@Qualifier("queueMessage") Queue queueMessage,
								   @Qualifier("exchange") TopicExchange exchange) {
		return BindingBuilder.bind(queueMessage).to(exchange).with(ROUTING_KEY);
	}
}
