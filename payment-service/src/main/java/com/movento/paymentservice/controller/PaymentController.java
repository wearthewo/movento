package com.movento.paymentservice.controller;

import com.movento.paymentservice.dto.ApiResponse;
import com.movento.paymentservice.dto.PaymentRequest;
import com.movento.paymentservice.dto.PaymentResponse;
import com.movento.paymentservice.model.Payment;
import com.movento.paymentservice.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<ApiResponse<PaymentResponse>> processPayment(
            @Valid @RequestBody PaymentRequest paymentRequest,
            HttpServletRequest request) {
        String clientIp = request.getRemoteAddr();
        PaymentResponse response = paymentService.processPayment(paymentRequest, clientIp);
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .status(200)
                .message("Payment processed successfully")
                .data(response)
                .build());
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPayment(@PathVariable String paymentId) {
        PaymentResponse response = paymentService.getPayment(paymentId);
        return ResponseEntity.ok(ApiResponse.<PaymentResponse>builder()
                .status(200)
                .message("Payment retrieved successfully")
                .data(response)
                .build());
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getUserPayments(@PathVariable Long userId) {
        List<Payment> payments = paymentService.getPaymentsByUserId(userId);
        List<PaymentResponse> paymentResponses = payments.stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.<List<PaymentResponse>>builder()
                .status(200)
                .message("User payments retrieved successfully")
                .data(paymentResponses)
                .build());
    }
    
    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .paymentIntentId(payment.getPaymentIntentId())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .status(payment.getStatus())
                .receiptUrl(payment.getReceiptUrl())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
