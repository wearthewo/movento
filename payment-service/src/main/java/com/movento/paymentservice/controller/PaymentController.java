package com.movento.paymentservice.controller;

import com.movento.paymentservice.dto.ApiResponse;
import com.movento.paymentservice.dto.PaymentRequest;
import com.movento.paymentservice.dto.PaymentResponse;
import com.movento.paymentservice.model.Payment;
import com.movento.paymentservice.model.elasticsearch.PaymentDocument;
import com.movento.paymentservice.service.PaymentService;
import com.movento.paymentservice.service.elasticsearch.PaymentSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentSearchService paymentSearchService;

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
    
    // Search endpoints
    
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<PaymentDocument>>> searchPayments(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Page<PaymentDocument> payments = paymentSearchService.searchPayments(query, page, size);
        return ResponseEntity.ok(ApiResponse.<Page<PaymentDocument>>builder()
                .status(200)
                .message("Payments searched successfully")
                .data(payments)
                .build());
    }
    
    @GetMapping("/search/amount")
    public ResponseEntity<ApiResponse<List<PaymentDocument>>> findPaymentsByAmountRange(
            @RequestParam BigDecimal minAmount,
            @RequestParam BigDecimal maxAmount) {
        List<PaymentDocument> payments = paymentSearchService.findPaymentsByAmountRange(minAmount, maxAmount);
        return ResponseEntity.ok(ApiResponse.<List<PaymentDocument>>builder()
                .status(200)
                .message("Payments found by amount range")
                .data(payments)
                .build());
    }
    
    @GetMapping("/search/status")
    public ResponseEntity<ApiResponse<List<PaymentDocument>>> findPaymentsByStatus(
            @RequestParam String status) {
        List<PaymentDocument> payments = paymentSearchService.findPaymentsByStatus(status);
        return ResponseEntity.ok(ApiResponse.<List<PaymentDocument>>builder()
                .status(200)
                .message("Payments found by status")
                .data(payments)
                .build());
    }
    
    @GetMapping("/search/user/{userId}")
    public ResponseEntity<ApiResponse<List<PaymentDocument>>> findPaymentsByUserId(
            @PathVariable String userId) {
        List<PaymentDocument> payments = paymentSearchService.findPaymentsByUserId(userId);
        return ResponseEntity.ok(ApiResponse.<List<PaymentDocument>>builder()
                .status(200)
                .message("Payments found by user ID")
                .data(payments)
                .build());
    }
    
    @GetMapping("/search/description")
    public ResponseEntity<ApiResponse<List<PaymentDocument>>> searchInDescription(
            @RequestParam String query) {
        List<PaymentDocument> payments = paymentSearchService.searchInDescription(query);
        return ResponseEntity.ok(ApiResponse.<List<PaymentDocument>>builder()
                .status(200)
                .message("Payments found by description")
                .data(payments)
                .build());
    }
    
    @GetMapping("/search/advanced")
    public ResponseEntity<ApiResponse<List<PaymentDocument>>> searchPaymentsAdvanced(
            @RequestParam String userId,
            @RequestParam double minAmount,
            @RequestParam double maxAmount,
            @RequestParam String status) {
        List<PaymentDocument> payments = paymentSearchService.searchPayments(userId, minAmount, maxAmount, status);
        return ResponseEntity.ok(ApiResponse.<List<PaymentDocument>>builder()
                .status(200)
                .message("Payments found by advanced search")
                .data(payments)
                .build());
    }
    
    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getPaymentStats() {
        Map<String, Long> stats = paymentSearchService.getPaymentStats();
        return ResponseEntity.ok(ApiResponse.<Map<String, Long>>builder()
                .status(200)
                .message("Payment statistics retrieved successfully")
                .data(stats)
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
