package com.movento.apigateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * One access-log line per request: method, path, status, duration and request id, e.g.
 * {@code GET /api/v1/catalog/home -> 200 in 14 ms [req=7e7fb208-…]}.
 * Request headers (with secrets masked) are logged only at DEBUG.
 */
@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "proxy-authorization", "cookie", "set-cookie", "stripe-signature");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String method = request.getMethod().name();
        String path = request.getURI().getPath();
        String requestId = request.getHeaders().getFirst("X-Request-Id");
        long started = System.nanoTime();
        if (logger.isDebugEnabled()) {
            logger.debug("{} {} headers={} [req={}]", method, path, filteredHeaders(request), requestId);
        }
        return chain.filter(exchange).doFinally(signal -> {
            var status = exchange.getResponse().getStatusCode();
            long millis = (System.nanoTime() - started) / 1_000_000;
            logger.info("{} {} -> {} in {} ms [req={}]", method, path, status == null ? "-" : status.value(), millis, requestId);
        });
    }

    private static String filteredHeaders(ServerHttpRequest request) {
        StringBuilder headers = new StringBuilder("{");
        request.getHeaders().forEach((name, values) -> {
            if (!SENSITIVE_HEADERS.contains(name.toLowerCase())) {
                headers.append(name).append('=').append(values.size() == 1 ? values.get(0) : values).append(", ");
            }
        });
        if (headers.length() > 1) headers.setLength(headers.length() - 2);
        return headers.append('}').toString();
    }

    @Override
    public int getOrder() {
        return -1; // after TrustedHeaderSanitizerFilter, so the request id is always set
    }
}
