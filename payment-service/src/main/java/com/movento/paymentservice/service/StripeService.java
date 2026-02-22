package com.movento.paymentservice.service;

import com.movento.paymentservice.dto.PaymentRequest;
import com.movento.paymentservice.dto.PaymentResponse;
import com.movento.paymentservice.exception.PaymentProcessingException;
import com.movento.paymentservice.model.Payment;
import com.stripe.Stripe;
import com.stripe.exception.*;
import com.stripe.model.Charge;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeService {

    @Value("${stripe.api.secret-key}")
    private String stripeApiKey;
    
    @Value("${stripe.currency.supported}")
    private List<String> supportedCurrencies;
    
    @Value("${stripe.payment.max-amount:1000000}")
    private long maxPaymentAmount;

    @PostConstruct
    public void init() {
        if (!StringUtils.hasText(stripeApiKey) || stripeApiKey.startsWith("sk_test")) {
            log.warn("Using test Stripe API key. Make sure to set STRIPE_SECRET_KEY in production.");
        }
        Stripe.apiKey = stripeApiKey;
        
        // Validate supported currencies
        Set<String> validCurrencies = Currency.getAvailableCurrencies().stream()
            .map(Currency::getCurrencyCode)
            .map(String::toLowerCase)
            .collect(Collectors.toSet());
            
        supportedCurrencies = supportedCurrencies.stream()
            .filter(validCurrencies::contains)
            .collect(Collectors.toList());
            
        if (supportedCurrencies.isEmpty()) {
            throw new IllegalStateException("No valid supported currencies configured");
        }
    }

    @Retryable(
        value = {StripeException.class},
        maxAttempts = 3,
        backoff = @Backoff(delay = 1000, multiplier = 2)
    )
    public PaymentResponse createPaymentIntent(PaymentRequest paymentRequest) {
        validatePaymentRequest(paymentRequest);
        
        try {
            String currency = paymentRequest.getCurrency().toLowerCase();
            long amount = convertToStripeAmount(paymentRequest.getAmount(), currency);
            
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amount)
                .setCurrency(currency)
                .setPaymentMethod(paymentRequest.getPaymentMethodId())
                .setConfirm(true)
                .setOffSession(true)
                .setConfirmationMethod(PaymentIntentCreateParams.ConfirmationMethod.MANUAL)
                .setDescription("Payment for plan: " + paymentRequest.getPlanId())
                .putMetadata("userId", paymentRequest.getUserId().toString())
                .putMetadata("planId", paymentRequest.getPlanId())
                .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);
            return mapToPaymentResponse(paymentIntent);
            
        } catch (CardException e) {
            log.error("Card error processing payment: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Card error: " + e.getLocalizedMessage(), 
                "card_error", HttpStatus.BAD_REQUEST);
                
        } catch (RateLimitException e) {
            log.error("Rate limit exceeded: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Rate limit exceeded. Please try again later.", 
                "rate_limit_error", HttpStatus.TOO_MANY_REQUESTS);
                
        } catch (InvalidRequestException e) {
            log.error("Invalid request to Stripe API: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Invalid payment request: " + e.getLocalizedMessage(), 
                "invalid_request_error", HttpStatus.BAD_REQUEST);
                
        } catch (AuthenticationException e) {
            log.error("Authentication with Stripe failed: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Payment processing is currently unavailable.", 
                "authentication_error", HttpStatus.INTERNAL_SERVER_ERROR);
                
        } catch (StripeException e) {
            log.error("Error processing payment: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Error processing payment. Please try again.", 
                "payment_error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Retryable(
        value = {StripeException.class},
        maxAttempts = 2,
        backoff = @Backoff(delay = 1000)
    )
    public PaymentResponse confirmPayment(String paymentIntentId) {
        if (!StringUtils.hasText(paymentIntentId)) {
            throw new PaymentProcessingException("Payment intent ID is required", 
                "invalid_payment_intent", HttpStatus.BAD_REQUEST);
        }
        
        try {
            PaymentIntent paymentIntent = PaymentIntent.retrieve(paymentIntentId);
            
            // Check if already succeeded
            if ("succeeded".equals(paymentIntent.getStatus())) {
                return mapToPaymentResponse(paymentIntent);
            }
            
            // Only confirm if not already confirmed
            if ("requires_confirmation".equals(paymentIntent.getStatus())) {
                paymentIntent = paymentIntent.confirm();
            }
            
            return mapToPaymentResponse(paymentIntent);
            
        } catch (InvalidRequestException e) {
            log.error("Invalid payment intent ID: {}", paymentIntentId, e);
            throw new PaymentProcessingException("Invalid payment intent ID", 
                "invalid_payment_intent", HttpStatus.NOT_FOUND);
                
        } catch (StripeException e) {
            log.error("Error confirming payment: {}", e.getMessage(), e);
            throw new PaymentProcessingException("Error confirming payment", 
                "confirmation_error", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private PaymentResponse mapToPaymentResponse(PaymentIntent paymentIntent) {
        if (paymentIntent == null) {
            throw new IllegalArgumentException("PaymentIntent cannot be null");
        }
        
        PaymentResponse response = new PaymentResponse();
        response.setPaymentIntentId(paymentIntent.getId());
        response.setClientSecret(paymentIntent.getClientSecret());
        
        if (paymentIntent.getAmount() != null) {
            response.setAmount(convertFromStripeAmount(
                paymentIntent.getAmount(), 
                paymentIntent.getCurrency()
            ));
        }
        
        if (paymentIntent.getCurrency() != null) {
            response.setCurrency(paymentIntent.getCurrency().toUpperCase());
        }
        
        response.setStatus(mapStatus(paymentIntent.getStatus()));
        response.setRequiresAction("requires_action".equals(paymentIntent.getStatus()));
        
        // Safely retrieve charge information if available
        if (paymentIntent.getLatestCharge() != null) {
            try {
                Charge charge = Charge.retrieve(paymentIntent.getLatestCharge());
                if (charge != null) {
                    response.setReceiptUrl(charge.getReceiptUrl());
                    response.setPaymentMethod(charge.getPaymentMethodDetails() != null ? 
                        charge.getPaymentMethodDetails().getType() : null);
                }
            } catch (StripeException e) {
                log.warn("Failed to retrieve charge details: {}", e.getMessage());
            }
        }
        
        // Include additional metadata if available
        if (paymentIntent.getMetadata() != null) {
            response.setMetadata(paymentIntent.getMetadata());
        }
        
        return response;
    }

    private void validatePaymentRequest(PaymentRequest paymentRequest) {
        if (paymentRequest == null) {
            throw new PaymentProcessingException("Payment request cannot be null", 
                "invalid_request", HttpStatus.BAD_REQUEST);
        }
        
        if (paymentRequest.getAmount() == null || paymentRequest.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new PaymentProcessingException("Invalid payment amount", 
                "invalid_amount", HttpStatus.BAD_REQUEST);
        }
        
        if (paymentRequest.getAmount().compareTo(BigDecimal.valueOf(maxPaymentAmount)) > 0) {
            throw new PaymentProcessingException("Payment amount exceeds maximum allowed", 
                "amount_too_large", HttpStatus.BAD_REQUEST);
        }
        
        if (!StringUtils.hasText(paymentRequest.getCurrency())) {
            throw new PaymentProcessingException("Currency is required", 
                "missing_currency", HttpStatus.BAD_REQUEST);
        }
        
        String currency = paymentRequest.getCurrency().toLowerCase();
        if (!supportedCurrencies.contains(currency)) {
            throw new PaymentProcessingException(
                String.format("Currency %s is not supported. Supported currencies: %s", 
                    currency, String.join(", ", supportedCurrencies)),
                "unsupported_currency", 
                HttpStatus.BAD_REQUEST);
        }
        
        if (!StringUtils.hasText(paymentRequest.getPaymentMethodId())) {
            throw new PaymentProcessingException("Payment method ID is required", 
                "missing_payment_method", HttpStatus.BAD_REQUEST);
        }
        
        if (paymentRequest.getUserId() == null) {
            throw new PaymentProcessingException("User ID is required", 
                "missing_user_id", HttpStatus.BAD_REQUEST);
        }
        
        if (!StringUtils.hasText(paymentRequest.getPlanId())) {
            throw new PaymentProcessingException("Plan ID is required", 
                "missing_plan_id", HttpStatus.BAD_REQUEST);
        }
    }
    
    private long convertToStripeAmount(BigDecimal amount, String currency) {
        // Stripe uses smallest currency unit (e.g., cents for USD)
        boolean isZeroDecimalCurrency = isZeroDecimalCurrency(currency);
        
        if (isZeroDecimalCurrency) {
            return amount.longValue();
        }
        
        try {
            return amount.multiply(BigDecimal.valueOf(100)).longValueExact();
        } catch (ArithmeticException e) {
            throw new PaymentProcessingException("Amount is too large", 
                "amount_too_large", HttpStatus.BAD_REQUEST);
        }
    }
    
    private boolean isZeroDecimalCurrency(String currency) {
        // List of zero-decimal currencies according to Stripe
        return Set.of("bif", "clp", "djf", "gnf", "jpy", "kmf", "krw",
                     "mga", "pyg", "rwf", "ugx", "vnd", "vuv", "xaf",
                     "xof", "xpf").contains(currency.toLowerCase());
    }

    private BigDecimal convertFromStripeAmount(long amount, String currency) {
        boolean isZeroDecimalCurrency = isZeroDecimalCurrency(currency);
        
        if (isZeroDecimalCurrency) {
            return BigDecimal.valueOf(amount);
        }
        
        return BigDecimal.valueOf(amount).divide(BigDecimal.valueOf(100));
    }

    private Payment.PaymentStatus mapStatus(String stripeStatus) {
        switch (stripeStatus) {
            case "requires_payment_method":
                return Payment.PaymentStatus.REQUIRES_PAYMENT_METHOD;
            case "requires_confirmation":
                return Payment.PaymentStatus.REQUIRES_CONFIRMATION;
            case "requires_action":
                return Payment.PaymentStatus.REQUIRES_ACTION;
            case "processing":
                return Payment.PaymentStatus.PROCESSING;
            case "requires_capture":
                return Payment.PaymentStatus.REQUIRES_CAPTURE;
            case "canceled":
                return Payment.PaymentStatus.CANCELED;
            case "succeeded":
                return Payment.PaymentStatus.SUCCEEDED;
            default:
                return Payment.PaymentStatus.FAILED;
        }
    }
}
