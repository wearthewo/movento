package com.movento.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

@RestController
public class FallbackController {

    @GetMapping("/fallback/user-service")
    public Mono<ResponseEntity<Map<String, Object>>> userServiceFallback() {
        return createFallbackResponse("User Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/fallback/content-service")
    public Mono<ResponseEntity<Map<String, Object>>> contentServiceFallback() {
        return createFallbackResponse("Content Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/fallback/streaming-service")
    public Mono<ResponseEntity<Map<String, Object>>> streamingServiceFallback() {
        return createFallbackResponse("Streaming Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/fallback/recommendation-service")
    public Mono<ResponseEntity<Map<String, Object>>> recommendationServiceFallback() {
        return createFallbackResponse("Recommendation Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/fallback/payment-service")
    public Mono<ResponseEntity<Map<String, Object>>> paymentServiceFallback() {
        return createFallbackResponse("Payment Service is currently unavailable. Please try again later.");
    }

    @GetMapping("/fallback")
    public Mono<ResponseEntity<Map<String, Object>>> defaultFallback() {
        return createFallbackResponse("Service is currently unavailable. Please try again later.");
    }

    private Mono<ResponseEntity<Map<String, Object>>> createFallbackResponse(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("error", "Service Unavailable");
        response.put("message", message);
        response.put("path", "/fallback");
        
        return Mono.just(ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(response));
    }
}
