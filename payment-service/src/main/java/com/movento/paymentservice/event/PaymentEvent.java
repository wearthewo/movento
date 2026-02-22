package com.movento.paymentservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentEvent {
    private String paymentId;
    private Long userId;
    private BigDecimal amount;
    private String status;
    private LocalDateTime timestamp = LocalDateTime.now();
}
