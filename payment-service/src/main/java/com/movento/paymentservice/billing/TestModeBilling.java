package com.movento.paymentservice.billing;

import com.movento.paymentservice.model.Payment;
import com.movento.paymentservice.model.Subscription;
import com.movento.paymentservice.repository.PaymentRepository;
import com.movento.paymentservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Simulated payments for local development and demos. No card data is collected or stored:
 * the caller only chooses the outcome ("success" or "decline"), mirroring Stripe's test cards.
 * Every record is marked test_* so it can never be mistaken for a real charge.
 */
@Service
@RequiredArgsConstructor
public class TestModeBilling {
    private static final Duration PERIOD = Duration.ofDays(30);

    private final SubscriptionRepository subscriptions;
    private final PaymentRepository payments;

    public enum Outcome { SUCCESS, DECLINE }

    @Transactional
    public Subscription subscribe(Long accountId, PlanCatalog.Plan plan, Outcome outcome) {
        Payment payment = new Payment();
        payment.setPaymentIntentId("test_pi_" + UUID.randomUUID().toString().replace("-", ""));
        payment.setUserId(accountId);
        payment.setPlanId(plan.id());
        payment.setAmount(plan.monthlyPrice());
        payment.setCurrency(plan.currency());
        payment.setCustomerId("test_cus_" + accountId);
        payment.setDescription("TEST MODE — " + plan.name() + " plan, 1 month (no real money)");
        payment.setStatus(outcome == Outcome.SUCCESS ? Payment.PaymentStatus.SUCCEEDED : Payment.PaymentStatus.FAILED);
        payments.save(payment);

        Subscription subscription = subscriptions.findById(accountId).orElseGet(() -> {
            Subscription created = new Subscription();
            created.setUserId(accountId);
            created.setPlanId(plan.id());
            return created;
        });
        if (outcome == Outcome.SUCCESS) {
            subscription.setPlanId(plan.id());
            subscription.setStatus(Subscription.Status.ACTIVE);
            subscription.setStripeCustomerId("test_cus_" + accountId);
            subscription.setCurrentPeriodEnd(Instant.now().plus(PERIOD));
            subscription.setCancelAtPeriodEnd(false);
        } else if (subscription.getStatus() == Subscription.Status.ACTIVE) {
            subscription.setStatus(Subscription.Status.PAST_DUE);
        }
        return subscriptions.save(subscription);
    }

    @Transactional
    public Subscription changePlan(Long accountId, PlanCatalog.Plan plan) {
        Subscription subscription = subscriptions.findById(accountId).orElseThrow();
        subscription.setPlanId(plan.id());
        return subscription;
    }

    @Transactional
    public Subscription setCancelAtPeriodEnd(Long accountId, boolean cancel) {
        Subscription subscription = subscriptions.findById(accountId).orElseThrow();
        subscription.setCancelAtPeriodEnd(cancel);
        return subscription;
    }
}
