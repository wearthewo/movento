package com.movento.paymentservice.service;

import com.movento.paymentservice.model.Payment;
import com.movento.paymentservice.model.Payment.PaymentStatus;
import com.movento.paymentservice.repository.PaymentRepository;
import com.movento.paymentservice.repository.SubscriptionRepository;
import com.movento.paymentservice.repository.WebhookEventRepository;
import com.movento.paymentservice.model.Subscription;
import com.movento.paymentservice.model.WebhookEvent;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.model.Charge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class StripeWebhookService {

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final WebhookEventRepository webhookEvents;

    @Transactional
    public void handleEvent(Event event) {
        if (webhookEvents.existsById(event.getId())) return;
        switch (event.getType()) {
            case "checkout.session.completed":
                handleCheckoutCompleted(event);
                break;
            case "customer.subscription.created":
            case "customer.subscription.updated":
            case "customer.subscription.deleted":
                handleSubscription(event);
                break;
            case "payment_intent.succeeded":
                handlePaymentIntentSucceeded(event);
                break;
            case "payment_intent.payment_failed":
                handlePaymentIntentFailed(event);
                break;
            case "charge.refunded":
                handleChargeRefunded(event);
                break;
            default:
                log.debug("Unhandled event type: {}", event.getType());
        }
        WebhookEvent processed = new WebhookEvent(); processed.setId(event.getId()); processed.setType(event.getType()); webhookEvents.save(processed);
    }

    private void handleCheckoutCompleted(Event event) {
        com.stripe.model.checkout.Session session = (com.stripe.model.checkout.Session) deserializeEventObject(event);
        if (session.getClientReferenceId() == null) return;
        Long accountId = Long.valueOf(session.getClientReferenceId());
        Subscription subscription = subscriptionRepository.findById(accountId).orElseGet(Subscription::new);
        subscription.setUserId(accountId); subscription.setPlanId("movento-monthly"); subscription.setStripeCustomerId(session.getCustomer()); subscription.setStripeSubscriptionId(session.getSubscription()); subscription.setStatus(Subscription.Status.ACTIVE);
        subscriptionRepository.save(subscription);
    }

    private void handleSubscription(Event event) {
        com.stripe.model.Subscription stripe = (com.stripe.model.Subscription) deserializeEventObject(event);
        subscriptionRepository.findByStripeCustomerId(stripe.getCustomer()).ifPresent(subscription -> {
            subscription.setStripeSubscriptionId(stripe.getId()); subscription.setCancelAtPeriodEnd(Boolean.TRUE.equals(stripe.getCancelAtPeriodEnd()));
            subscription.setStatus(mapStatus(stripe.getStatus()));
            subscriptionRepository.save(subscription);
        });
    }

    private Subscription.Status mapStatus(String status) {
        if (status == null) return Subscription.Status.INACTIVE;
        try { return Subscription.Status.valueOf(status.toUpperCase()); } catch (IllegalArgumentException ignored) { return Subscription.Status.INACTIVE; }
    }

    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) deserializeEventObject(event);
        String receiptUrl = paymentIntent.getLatestChargeObject() != null ? 
            paymentIntent.getLatestChargeObject().getReceiptUrl() : null;
        updatePaymentStatus(paymentIntent.getId(), PaymentStatus.SUCCEEDED, receiptUrl);
    }

    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) deserializeEventObject(event);
        updatePaymentStatus(paymentIntent.getId(), PaymentStatus.FAILED, null);
    }

    private void handleChargeRefunded(Event event) {
        Charge charge = (Charge) deserializeEventObject(event);
        updatePaymentStatus(charge.getPaymentIntent(), PaymentStatus.REFUNDED, null);
    }

    private void updatePaymentStatus(String paymentIntentId, PaymentStatus status, String receiptUrl) {
        paymentRepository.findByPaymentIntentId(paymentIntentId).ifPresent(payment -> {
            payment.setStatus(status);
            if (receiptUrl != null) {
                payment.setReceiptUrl(receiptUrl);
            }
            paymentRepository.save(payment);
            log.info("Updated payment {} to status: {}", paymentIntentId, status);
        });
    }

    private StripeObject deserializeEventObject(Event event) {
        var deserializer = event.getDataObjectDeserializer();
        return deserializer.getObject().orElseGet(() -> {
            try {
                // The webhook signature has already been verified. This fallback handles
                // events created with a newer Stripe API version than the pinned SDK.
                return deserializer.deserializeUnsafe();
            } catch (EventDataObjectDeserializationException exception) {
                throw new IllegalStateException("Unable to deserialize Stripe event " + event.getId(), exception);
            }
        });
    }
}
