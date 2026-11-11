package com.kumao.live.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${kumao.mq.danmaku-exchange:kumao.live.exchange}")
    private String exchange;
    @Value("${kumao.mq.danmaku-queue:kumao.live.danmaku}")
    private String queue;
    @Value("${kumao.mq.danmaku-routing-key:live.danmaku}")
    private String routingKey;

    @Bean
    public DirectExchange danmakuExchange() {
        return new DirectExchange(exchange, true, false);
    }

    @Bean
    public Queue danmakuQueue() {
        return QueueBuilder.durable(queue).build();
    }

    @Bean
    public Binding danmakuBinding(Queue danmakuQueue, DirectExchange danmakuExchange) {
        return BindingBuilder.bind(danmakuQueue).to(danmakuExchange).with(routingKey);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
