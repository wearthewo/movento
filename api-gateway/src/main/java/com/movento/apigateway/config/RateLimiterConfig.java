package com.movento.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Redis token-bucket rate limiting (shared across gateway instances).
 * Per-route limits are set in application.yml; this is only the default bucket.
 */
@Configuration
public class RateLimiterConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        return new RedisRateLimiter(10, 20, 1);
    }

    /**
     * Keys anonymous traffic (sign-in, sign-up, password reset) by client IP.
     * The gateway is never public: the Next.js BFF calls it and forwards the browser IP as the
     * last X-Forwarded-For entry (the value appended by the trusted reverse proxy), so take the last one.
     */
    @Bean
    @Primary
    public KeyResolver clientIpKeyResolver() {
        return exchange -> Mono.just("ip:" + clientIp(exchange));
    }

    /** For authenticated routes: one bucket per account (X-Account-Id is set by the gateway, never by clients). */
    @Bean
    public KeyResolver accountKeyResolver() {
        return exchange -> {
            String accountId = exchange.getRequest().getHeaders().getFirst("X-Account-Id");
            return Mono.just(accountId != null && !accountId.isBlank() ? "account:" + accountId : "ip:" + clientIp(exchange));
        };
    }

    static String clientIp(ServerWebExchange exchange) {
        String forwarded = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            String[] hops = forwarded.split(",");
            return hops[hops.length - 1].trim();
        }
        var remote = exchange.getRequest().getRemoteAddress();
        return remote == null || remote.getAddress() == null ? "unknown" : remote.getAddress().getHostAddress();
    }
}
