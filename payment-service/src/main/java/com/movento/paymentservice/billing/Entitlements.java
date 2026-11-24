package com.movento.paymentservice.billing;

import com.movento.paymentservice.model.Subscription;

import java.time.Instant;

public final class Entitlements {
    private Entitlements() {
    }

    /** Active or trialing, and the paid period has not run out (a cancelled plan keeps working until period end). */
    public static boolean isActive(Subscription subscription) {
        boolean paid = subscription.getStatus() == Subscription.Status.ACTIVE || subscription.getStatus() == Subscription.Status.TRIALING;
        return paid && (subscription.getCurrentPeriodEnd() == null || subscription.getCurrentPeriodEnd().isAfter(Instant.now()));
    }
}
