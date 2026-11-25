package com.movento.paymentservice.controller;

import com.movento.paymentservice.billing.Entitlements;
import com.movento.paymentservice.billing.PlanCatalog;
import com.movento.paymentservice.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Service-to-service entitlement check used by streaming-service before issuing a playback session.
 * Not routed by the API gateway, so browsers cannot reach it.
 */
@lombok.extern.slf4j.Slf4j
@RestController
@RequestMapping("/internal/entitlements")
@RequiredArgsConstructor
public class InternalEntitlementController {
    private final SubscriptionRepository subscriptions;

    @GetMapping("/{accountId}")
    public Map<String, Object> entitlement(@PathVariable Long accountId) {
        Map<String, Object> result = subscriptions.findById(accountId)
                .map(s -> Map.<String, Object>of(
                        "active", Entitlements.isActive(s),
                        "planId", s.getPlanId(),
                        "maxStreams", PlanCatalog.find(s.getPlanId()).map(PlanCatalog.Plan::maxStreams).orElse(1)))
                .orElseGet(() -> Map.of("active", false, "planId", "", "maxStreams", 0));
        log.info("Entitlement check account={} active={} plan={}", accountId, result.get("active"), result.get("planId"));
        return result;
    }
}
