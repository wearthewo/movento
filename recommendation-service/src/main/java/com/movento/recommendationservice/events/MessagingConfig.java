package com.movento.recommendationservice.events;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Topology owned by this consumer:
 * <pre>
 * streaming.events (topic) --playback.started--> recommendation.playback-started
 *                                                  | after retries are exhausted
 *                                                  v
 *                           recommendation.dlx --> recommendation.playback-started.dlq
 * </pre>
 * Declaring is idempotent, so it does not matter whether producer or consumer starts first.
 */
@Configuration
public class MessagingConfig {
    public static final String QUEUE = "recommendation.playback-started";
    public static final String DEAD_LETTER_EXCHANGE = "recommendation.dlx";
    public static final String DEAD_LETTER_QUEUE = "recommendation.playback-started.dlq";

    @Bean
    public TopicExchange streamingEvents(@Value("${app.messaging.streaming-exchange:streaming.events}") String name) {
        return new TopicExchange(name, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DEAD_LETTER_EXCHANGE, true, false);
    }

    @Bean
    public Queue playbackStartedQueue() {
        return QueueBuilder.durable(QUEUE)
                .deadLetterExchange(DEAD_LETTER_EXCHANGE)
                .deadLetterRoutingKey(DEAD_LETTER_QUEUE)
                .build();
    }

    @Bean
    public Queue playbackStartedDeadLetterQueue() {
        return QueueBuilder.durable(DEAD_LETTER_QUEUE).build();
    }

    @Bean
    public Binding playbackStartedBinding(Queue playbackStartedQueue, TopicExchange streamingEvents) {
        return BindingBuilder.bind(playbackStartedQueue).to(streamingEvents).with("playback.started");
    }

    @Bean
    public Binding deadLetterBinding(Queue playbackStartedDeadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(playbackStartedDeadLetterQueue).to(deadLetterExchange).with(DEAD_LETTER_QUEUE);
    }

    /** JSON in, converted to the listener's parameter type (ignores the producer's Java class name header). */
    @Bean
    public MessageConverter jsonMessageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter(objectMapper);
        converter.setAlwaysConvertToInferredType(true);
        return converter;
    }
}
