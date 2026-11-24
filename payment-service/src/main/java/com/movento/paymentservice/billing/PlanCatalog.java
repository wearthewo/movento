package com.movento.paymentservice.billing;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/** Netflix-style plan tiers. Prices are display/test values only — no real money is ever charged in test mode. */
public final class PlanCatalog {

    public record Plan(String id, String name, BigDecimal monthlyPrice, String currency, String videoQuality,
                       int maxStreams, List<String> features) {}

    public static final List<Plan> PLANS = List.of(
            new Plan("basic", "Basic", new BigDecimal("7.99"), "EUR", "HD (720p)", 1,
                    List.of("Watch on 1 screen at a time", "HD video", "Unlimited films and series")),
            new Plan("standard", "Standard", new BigDecimal("13.99"), "EUR", "Full HD (1080p)", 2,
                    List.of("Watch on 2 screens at a time", "Full HD video", "Up to 5 profiles")),
            new Plan("premium", "Premium", new BigDecimal("19.99"), "EUR", "4K + HDR", 4,
                    List.of("Watch on 4 screens at a time", "4K Ultra HD + HDR", "Spatial audio")));

    private PlanCatalog() {
    }

    public static Optional<Plan> find(String id) {
        return PLANS.stream().filter(plan -> plan.id().equals(id)).findFirst();
    }
}
