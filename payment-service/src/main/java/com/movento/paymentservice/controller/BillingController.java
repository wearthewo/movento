package com.movento.paymentservice.controller;

import com.movento.paymentservice.model.Subscription;
import com.movento.paymentservice.repository.SubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController @RequestMapping("/api/v1/billing") @RequiredArgsConstructor
public class BillingController {
    private final SubscriptionRepository subscriptions;
    @Value("${stripe.price-id:}") private String priceId;
    @Value("${app.web-url:http://localhost:3000}") private String webUrl;

    @GetMapping("/subscription") public Subscription subscription(@RequestHeader("X-Account-Id") Long accountId) {
        return subscriptions.findById(accountId).orElseGet(() -> { Subscription s = new Subscription(); s.setUserId(accountId); s.setPlanId("movento-monthly"); return s; });
    }

    @PostMapping("/checkout") public Map<String, String> checkout(@RequestHeader("X-Account-Id") Long accountId, @RequestHeader("X-User-Email") String email) throws StripeException {
        if (priceId.isBlank()) return Map.of("url", webUrl + "/account?billing=demo");
        SessionCreateParams params = SessionCreateParams.builder().setMode(SessionCreateParams.Mode.SUBSCRIPTION).setCustomerEmail(email)
            .setClientReferenceId(String.valueOf(accountId)).putMetadata("accountId", String.valueOf(accountId))
            .addLineItem(SessionCreateParams.LineItem.builder().setPrice(priceId).setQuantity(1L).build())
            .setSuccessUrl(webUrl + "/account?billing=success").setCancelUrl(webUrl + "/account?billing=cancelled").build();
        return Map.of("url", Session.create(params).getUrl());
    }

    @PostMapping("/portal") public Map<String, String> portal(@RequestHeader("X-Account-Id") Long accountId) throws StripeException {
        Subscription subscription = subscriptions.findById(accountId).orElseThrow();
        com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams.builder().setCustomer(subscription.getStripeCustomerId()).setReturnUrl(webUrl + "/account").build();
        return Map.of("url", com.stripe.model.billingportal.Session.create(params).getUrl());
    }
}
