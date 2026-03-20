package cc.ivera;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    //topic模式

    public static final String QUEUE_NAME="boot_queue";

    public static final String EXCHANGE_NAME="exchange_name";


    /**
     * 不指定队列名名称程序自动创建一个队列
     * @return
     */
    @Bean("bootQueue")
    public Queue queue(){
        return QueueBuilder.durable(QUEUE_NAME).build();
    }

    @Bean("bootExchange")
    public Exchange exchange(){
        return ExchangeBuilder.topicExchange(EXCHANGE_NAME).durable(true).build();
    }


    @Bean
    public Binding bindQueueExchange(@Qualifier("bootQueue") Queue queue,@Qualifier("bootExchange") Exchange exchange){
      return BindingBuilder.bind(queue).to(exchange).with("boot.#").noargs();
    }


}
