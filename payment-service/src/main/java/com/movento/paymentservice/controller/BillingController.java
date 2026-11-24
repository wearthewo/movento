package com.movento.paymentservice.controller;

import com.movento.paymentservice.billing.Entitlements;
import com.movento.paymentservice.billing.PlanCatalog;
import com.movento.paymentservice.billing.TestModeBilling;
import com.movento.paymentservice.model.Payment;
import com.movento.paymentservice.model.Subscription;
import com.movento.paymentservice.repository.PaymentRepository;
import com.movento.paymentservice.repository.SubscriptionRepository;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Billing for the signed-in account (X-Account-Id / X-User-Email come from the gateway).
 * Without STRIPE_PRICE_ID the service runs in test mode: plans are "paid" with simulated payments.
 * With Stripe test keys configured, checkout goes through real Stripe Checkout in test mode.
 */
@RestController
@RequestMapping("/api/v1/billing")
@RequiredArgsConstructor
public class BillingController {
    private final SubscriptionRepository subscriptions;
    private final PaymentRepository payments;
    private final TestModeBilling testBilling;

    @Value("${stripe.price-id:}")
    private String priceId;

    @Value("${app.web-url:http://localhost:3000}")
    private String webUrl;

    public record CheckoutRequest(@NotBlank String planId, String outcome) {}
    public record ChangePlanRequest(@NotBlank String planId) {}
    public record SubscriptionView(String planId, String planName, String status, Instant currentPeriodEnd,
                                   boolean cancelAtPeriodEnd, boolean active, int maxStreams, String videoQuality, boolean testMode) {}
    public record PaymentView(String id, String planId, BigDecimal amount, String currency, String status, String description, String createdAt) {}

    private boolean testMode() {
        return priceId == null || priceId.isBlank();
    }

    @GetMapping("/plans")
    public Map<String, Object> plans() {
        return Map.of("plans", PlanCatalog.PLANS, "testMode", testMode());
    }

    @GetMapping("/subscription")
    public SubscriptionView subscription(@RequestHeader("X-Account-Id") Long accountId) {
        return subscriptions.findById(accountId).map(this::view)
                .orElseGet(() -> new SubscriptionView(null, null, "INACTIVE", null, false, false, 0, null, testMode()));
    }

    @GetMapping("/payments")
    public List<PaymentView> history(@RequestHeader("X-Account-Id") Long accountId) {
        return payments.findByUserId(accountId).stream()
                .sorted(Comparator.comparing(Payment::getCreatedAt, Comparator.nullsLast(Comparator.<java.time.LocalDateTime>reverseOrder())))
                .limit(24)
                .map(p -> new PaymentView(p.getPaymentIntentId(), p.getPlanId(), p.getAmount(), p.getCurrency(), p.getStatus().name(),
                        p.getDescription(), p.getCreatedAt() == null ? null : p.getCreatedAt().toString()))
                .toList();
    }

    @PostMapping("/checkout")
    public Map<String, String> checkout(@RequestHeader("X-Account-Id") Long accountId, @RequestHeader("X-User-Email") String email,
                                        @Valid @RequestBody CheckoutRequest request) throws StripeException {
        PlanCatalog.Plan plan = plan(request.planId());
        if (testMode()) {
            TestModeBilling.Outcome outcome = "decline".equalsIgnoreCase(request.outcome()) ? TestModeBilling.Outcome.DECLINE : TestModeBilling.Outcome.SUCCESS;
            Subscription result = testBilling.subscribe(accountId, plan, outcome);
            boolean ok = outcome == TestModeBilling.Outcome.SUCCESS;
            return Map.of("status", ok ? result.getStatus().name() : "DECLINED", "url", webUrl + "/account?billing=" + (ok ? "success" : "declined"));
        }
        SessionCreateParams params = SessionCreateParams.builder().setMode(SessionCreateParams.Mode.SUBSCRIPTION).setCustomerEmail(email)
                .setClientReferenceId(String.valueOf(accountId)).putMetadata("accountId", String.valueOf(accountId)).putMetadata("planId", plan.id())
                .addLineItem(SessionCreateParams.LineItem.builder().setPrice(priceId).setQuantity(1L).build())
                .setSuccessUrl(webUrl + "/account?billing=success").setCancelUrl(webUrl + "/account?billing=cancelled").build();
        return Map.of("status", "REDIRECT", "url", Session.create(params).getUrl());
    }

    @PostMapping("/change-plan")
    public SubscriptionView changePlan(@RequestHeader("X-Account-Id") Long accountId, @Valid @RequestBody ChangePlanRequest request) {
        requireTestMode();
        return view(testBilling.changePlan(accountId, plan(request.planId())));
    }

    @PostMapping("/cancel")
    public SubscriptionView cancel(@RequestHeader("X-Account-Id") Long accountId) {
        requireTestMode();
        return view(testBilling.setCancelAtPeriodEnd(accountId, true));
    }

    @PostMapping("/resume")
    public SubscriptionView resume(@RequestHeader("X-Account-Id") Long accountId) {
        requireTestMode();
        return view(testBilling.setCancelAtPeriodEnd(accountId, false));
    }

    @PostMapping("/portal")
    public Map<String, String> portal(@RequestHeader("X-Account-Id") Long accountId) throws StripeException {
        requireStripe();
        Subscription subscription = subscriptions.findById(accountId).orElseThrow();
        com.stripe.param.billingportal.SessionCreateParams params = com.stripe.param.billingportal.SessionCreateParams.builder()
                .setCustomer(subscription.getStripeCustomerId()).setReturnUrl(webUrl + "/account").build();
        return Map.of("url", com.stripe.model.billingportal.Session.create(params).getUrl());
    }

    private void requireTestMode() {
        if (!testMode()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Manage your Stripe subscription from the billing portal.");
    }

    private void requireStripe() {
        if (testMode()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Billing portal is unavailable in test mode.");
    }

    private static PlanCatalog.Plan plan(String id) {
        return PlanCatalog.find(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown plan " + id));
    }

    private SubscriptionView view(Subscription s) {
        PlanCatalog.Plan plan = PlanCatalog.find(s.getPlanId()).orElse(null);
        return new SubscriptionView(s.getPlanId(), plan == null ? s.getPlanId() : plan.name(), s.getStatus().name(), s.getCurrentPeriodEnd(),
                s.isCancelAtPeriodEnd(), Entitlements.isActive(s), plan == null ? 1 : plan.maxStreams(), plan == null ? null : plan.videoQuality(), testMode());
    }
}
