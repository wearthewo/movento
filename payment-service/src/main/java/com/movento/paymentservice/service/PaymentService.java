package com.movento.paymentservice.service;

import java.util.Map;
import java.util.Collections;
import com.fasterxml.jackson.core.type.TypeReference;
import com.movento.paymentservice.dto.PaymentRequest;
import com.movento.paymentservice.dto.PaymentResponse;
import com.movento.paymentservice.model.elasticsearch.PaymentDocument;
import com.movento.paymentservice.exception.PaymentProcessingException;
import com.movento.paymentservice.exception.ResourceNotFoundException;
import com.movento.paymentservice.model.Payment;
import com.movento.paymentservice.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.UUID;

import com.movento.paymentservice.service.elasticsearch.PaymentSearchService;
import com.movento.paymentservice.service.elasticsearch.PaymentAuditService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final StripeService stripeService;
    private final PaymentRepository paymentRepository;
    private final PaymentSearchService paymentSearchService;
    private final PaymentAuditService paymentAuditService;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Value("${rabbitmq.payment.exchange}")
    private String paymentExchange;

    @Value("${rabbitmq.payment.routing-key}")
    private String paymentRoutingKey;

    @Cacheable(value = "payments", key = "#paymentId")
    public PaymentResponse getPayment(String paymentId) {
        Payment payment = paymentRepository.findByPaymentIntentId(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with id: " + paymentId));
        return mapToPaymentResponse(payment);
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentIntentId(payment.getPaymentIntentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .receiptUrl(payment.getReceiptUrl())
                .createdAt(payment.getCreatedAt())
                .requiresAction(false)
                .build();
    }

    @Transactional
    public PaymentResponse processPayment(PaymentRequest paymentRequest, String ipAddress) {
        try {
            // Process payment with Stripe
            PaymentResponse response = stripeService.createPaymentIntent(paymentRequest);
            
            // Save payment record
            Payment payment = new Payment();
            payment.setPaymentIntentId(response.getPaymentIntentId());
            payment.setUserId(paymentRequest.getUserId());
            payment.setPlanId(paymentRequest.getPlanId());
            payment.setAmount(response.getAmount());
            payment.setCurrency(response.getCurrency());
            payment.setStatus(response.getStatus());
            payment.setReceiptUrl(response.getReceiptUrl());
            
            payment = paymentRepository.save(payment);
            
            // Index payment in Elasticsearch
            indexPayment(payment);
            
            // Log audit event
            logPaymentEvent(payment, "PAYMENT_CREATED", null, paymentRequest, ipAddress);
            
            // Publish payment processed event
            publishPaymentProcessedEvent(payment);
            
            return response;
            
        } catch (Exception e) {
            log.error("Error processing payment: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Failed to process payment: " + e.getMessage());
        }
    }

    @Cacheable(value = "payments", key = "#paymentIntentId", unless = "#result == null")
    @Transactional(readOnly = true)
    public Payment getPaymentByIntentId(String paymentIntentId) {
        return paymentRepository.findByPaymentIntentId(paymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found with ID: " + paymentIntentId));
    }

    @Transactional(readOnly = true)
    public List<Payment> getPaymentsByUserId(Long userId) {
        return paymentRepository.findByUserId(userId);
    }

    @CacheEvict(value = "payments", key = "#paymentIntentId")
    @Transactional
    public Payment updatePaymentStatus(String paymentIntentId, Payment.PaymentStatus status, String receiptUrl, String ipAddress) {
        Payment oldPayment = getPaymentByIntentId(paymentIntentId);
        Payment.PaymentStatus oldStatus = oldPayment.getStatus();
        
        oldPayment.setStatus(status);
        if (receiptUrl != null) {
            oldPayment.setReceiptUrl(receiptUrl);
        }
        
        Payment updatedPayment = paymentRepository.save(oldPayment);
        
        // Update the payment in Elasticsearch
        indexPayment(updatedPayment);
        
        // Log the status change
        logPaymentEvent(updatedPayment, "PAYMENT_UPDATED", oldPayment, 
                       Map.of("status", status.name(), "oldStatus", oldStatus.name()), 
                       ipAddress);
        
        return updatedPayment;
    }
    
    private void indexPayment(Payment payment) {
        try {
            PaymentDocument paymentDoc = PaymentDocument.builder()
                .id(payment.getId() != null ? payment.getId().toString() : UUID.randomUUID().toString())
                .paymentIntentId(payment.getPaymentIntentId())
                .status(payment.getStatus().name())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .userId(payment.getUserId().toString())
                .planId(payment.getPlanId())
                .createdAt(payment.getCreatedAt())
                .description("Payment for plan: " + payment.getPlanId())
                .build();
            
            paymentSearchService.indexPayment(paymentDoc);
        } catch (Exception e) {
            log.error("Failed to index payment: {}", payment.getPaymentIntentId(), e);
        }
    }
    
    private void logPaymentEvent(Payment payment, String eventType, 
                               Payment oldPayment, Object newValues, String ipAddress) {
        try {
            Map<String, Object> oldValuesMap = oldPayment != null ? 
                objectMapper.convertValue(oldPayment, new TypeReference<>() {}) : 
                Collections.emptyMap();
                
            paymentAuditService.logPaymentEvent(
                payment.getPaymentIntentId(),
                eventType,
                oldValuesMap,
                objectMapper.convertValue(newValues, new TypeReference<>() {}),
                payment.getUserId() != null ? payment.getUserId().toString() : "system",
                ipAddress,
                String.format("Payment %s: %s", eventType, payment.getPaymentIntentId()),
                Map.of("amount", payment.getAmount() != null ? payment.getAmount().toString() : "0",
                      "currency", payment.getCurrency() != null ? payment.getCurrency() : "")
            );
        } catch (Exception e) {
            log.error("Failed to log payment event", e);
        }
    }
    
    private void publishPaymentProcessedEvent(Payment payment) {
        try {
            String message = String.format(
                "Payment %s processed for user %s, amount: %s %s",
                payment.getPaymentIntentId(),
                payment.getUserId(),
                payment.getAmount(),
                payment.getCurrency()
            );
            rabbitTemplate.convertAndSend(paymentExchange, paymentRoutingKey, message);
        } catch (Exception e) {
            log.error("Failed to publish payment processed event", e);
        }
    }
}
