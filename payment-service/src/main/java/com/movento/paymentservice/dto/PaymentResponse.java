package com.movento.paymentservice.dto;

import com.movento.paymentservice.model.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentResponse {
    private String paymentIntentId;
    private String clientSecret;
    private BigDecimal amount;
    private String currency;
    private Payment.PaymentStatus status;
    private String receiptUrl;
    private boolean requiresAction;
    private String paymentMethod;
    private LocalDateTime createdAt;
    private Map<String, String> metadata;
}
