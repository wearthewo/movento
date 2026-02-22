package com.movento.paymentservice.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

@Service
public class MetricsService {
    private final MeterRegistry meterRegistry;
    private Counter paymentCounter;
    private Counter paymentErrorCounter;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void init() {
        paymentCounter = Counter.builder("payment_processed_total")
            .description("Total number of payments processed")
            .register(meterRegistry);
            
        paymentErrorCounter = Counter.builder("payment_errors_total")
            .description("Total number of payment processing errors")
            .register(meterRegistry);
    }

    public void incrementPaymentCounter() {
        paymentCounter.increment();
    }

    public void incrementErrorCounter() {
        paymentErrorCounter.increment();
    }
}
