package com.movento.paymentservice.service;

import com.movento.paymentservice.event.PaymentEvent;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PaymentEventPublisher {
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.payment.exchange}")
    private String paymentExchange;
    
    @Value("${rabbitmq.payment.routing-key}")
    private String paymentRoutingKey;

    public PaymentEventPublisher(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void publishPaymentEvent(PaymentEvent event) {
        rabbitTemplate.convertAndSend(paymentExchange, paymentRoutingKey, event);
    }
}
