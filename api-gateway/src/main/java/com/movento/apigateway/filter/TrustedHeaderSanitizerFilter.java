package com.movento.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * Removes identity headers that only the gateway is allowed to set.
 *
 * Downstream services trust X-Account-Id, X-Profile-Id, etc. Without this filter a
 * client could send those headers on routes that do not run {@link AuthenticationFilter}
 * (catalog, auth, webhooks) and impersonate another account. Runs before every route.
 * Also guarantees every request carries an X-Request-Id for log correlation.
 */
@Component
public class TrustedHeaderSanitizerFilter implements GlobalFilter, Ordered {

    public static final List<String> TRUSTED_HEADERS = List.of(
            "X-Account-Id", "X-Profile-Id", "X-Profile-Maturity", "X-User-Email",
            "X-User-Roles", "X-Subscription-Status");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String requestId = exchange.getRequest().getHeaders().getFirst("X-Request-Id");
        String safeRequestId = requestId != null && requestId.matches("[A-Za-z0-9-]{8,64}")
                ? requestId : UUID.randomUUID().toString();
        ServerHttpRequest sanitized = exchange.getRequest().mutate()
                .headers(headers -> {
                    TRUSTED_HEADERS.forEach(headers::remove);
                    headers.set("X-Request-Id", safeRequestId);
                })
                .build();
        exchange.getResponse().getHeaders().set("X-Request-Id", safeRequestId);
        return chain.filter(exchange.mutate().request(sanitized).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
