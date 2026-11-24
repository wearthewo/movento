package com.movento.streamingservice.media;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

/** Asks payment-service (via Eureka) whether an account has an active plan. Fails closed. */
@Component
public class EntitlementClient {
    private final RestTemplate http;
    private final String paymentServiceUrl;

    public EntitlementClient(RestTemplate loadBalancedRestTemplate, @Value("${app.payment-service-url:http://payment-service}") String paymentServiceUrl) {
        this.http = loadBalancedRestTemplate;
        this.paymentServiceUrl = paymentServiceUrl;
    }

    public boolean hasActivePlan(Long accountId) {
        try {
            Map<?, ?> body = http.getForObject(paymentServiceUrl + "/internal/entitlements/{id}", Map.class, accountId);
            return body != null && Boolean.TRUE.equals(body.get("active"));
        } catch (RestClientException unavailable) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Billing is temporarily unavailable. Please try again.");
        }
    }
}
