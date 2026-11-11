package com.kumao.comment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${kumao.mq.play-record-exchange:kumao.play.exchange}")
    private String exchange;
    @Value("${kumao.mq.play-record-queue:kumao.play.record}")
    private String queue;
    @Value("${kumao.mq.play-record-routing-key:play.record}")
    private String routingKey;

    @Bean
    public DirectExchange playRecordExchange() {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    public Queue playRecordQueue() {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding playRecordBinding(Queue playRecordQueue, DirectExchange playRecordExchange) {
        return BindingBuilder.bind(playRecordQueue).to(playRecordExchange).with(routingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
