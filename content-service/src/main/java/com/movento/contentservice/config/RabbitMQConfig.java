package com.movento.contentservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableRabbit
public class RabbitMQConfig {
    // Content service queues and exchanges
    public static final String CONTENT_QUEUE = "content.queue";
    public static final String CONTENT_EXCHANGE = "content.exchange";
    public static final String CONTENT_ROUTING_KEY = "content.routing.key";

    // Payment service queues and exchanges
    public static final String PAYMENT_QUEUE = "payment.queue";
    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String PAYMENT_ROUTING_KEY = "payment.processed";

    // Content service beans
    @Bean
    public Queue contentQueue() {
        return new Queue(CONTENT_QUEUE, true);
    }

    @Bean
    public TopicExchange contentExchange() {
        return new TopicExchange(CONTENT_EXCHANGE);
    }

    @Bean
    public Binding contentBinding() {
        return BindingBuilder
                .bind(contentQueue())
                .to(contentExchange())
                .with(CONTENT_ROUTING_KEY);
    }

    // Payment service beans
    @Bean
    public Queue paymentQueue() {
        return new Queue(PAYMENT_QUEUE, true);
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Binding paymentBinding() {
        return BindingBuilder
                .bind(paymentQueue())
                .to(paymentExchange())
                .with(PAYMENT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

}
