package com.movento.paymentservice.dto;

import lombok.Data;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class PaymentRequest {
    @NotNull(message = "User ID is required")
    private Long userId;

    @NotBlank(message = "Plan ID is required")
    private String planId;

    @NotBlank(message = "Payment method ID is required")
    private String paymentMethodId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    private String currency;

    private boolean savePaymentMethod = false;
}
