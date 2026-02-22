package com.movento.recommendationservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public TopicExchange recommendationExchange(@Value("${app.messaging.exchange}") String exchangeName) {
        return new TopicExchange(exchangeName);
    }
}
