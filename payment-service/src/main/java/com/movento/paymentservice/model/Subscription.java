package com.movento.paymentservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.Instant;

@Entity @Table(name = "subscriptions") @Getter @Setter
public class Subscription {
    @Id private Long userId;
    @Column(unique = true) private String stripeCustomerId;
    @Column(unique = true) private String stripeSubscriptionId;
    @Column(nullable = false) private String planId;
    @Enumerated(EnumType.STRING) @Column(nullable = false) private Status status = Status.INACTIVE;
    private Instant currentPeriodEnd;
    private boolean cancelAtPeriodEnd;
    public enum Status { INACTIVE, TRIALING, ACTIVE, PAST_DUE, CANCELED, UNPAID }
}
