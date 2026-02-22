package com.movento.paymentservice.service;

import com.movento.paymentservice.model.Payment;
import com.movento.paymentservice.model.Payment.PaymentStatus;
import com.movento.paymentservice.repository.PaymentRepository;
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

    @Transactional
    public void handleEvent(Event event) {
        switch (event.getType()) {
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
    }

    private void handlePaymentIntentSucceeded(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        String receiptUrl = paymentIntent.getLatestChargeObject() != null ? 
            paymentIntent.getLatestChargeObject().getReceiptUrl() : null;
        updatePaymentStatus(paymentIntent.getId(), PaymentStatus.SUCCEEDED, receiptUrl);
    }

    private void handlePaymentIntentFailed(Event event) {
        PaymentIntent paymentIntent = (PaymentIntent) event.getData().getObject();
        updatePaymentStatus(paymentIntent.getId(), PaymentStatus.FAILED, null);
    }

    private void handleChargeRefunded(Event event) {
        Charge charge = (Charge) event.getData().getObject();
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
}
