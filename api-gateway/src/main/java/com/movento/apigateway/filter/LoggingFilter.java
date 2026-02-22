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

import java.net.URI;
import java.util.Collections;
import java.util.Set;

@Component
public class LoggingFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(LoggingFilter.class);
    private static final Set<String> SENSITIVE_HEADERS = Set.of("authorization", "proxy-authorization");
    private static final String REQUEST_LOG_FORMAT = "Incoming request: {} {} - Headers: {}";
    private static final String RESPONSE_LOG_FORMAT = "Response for {} {} - Status: {}";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        URI uri = request.getURI();
        String method = request.getMethod().name();
        
        // Log request
        logger.info(REQUEST_LOG_FORMAT, 
            method, 
            uri.getPath(),
            getFilteredHeaders(request)
        );
        
        // Log response
        return chain.filter(exchange).doFinally(signalType -> {
            logger.info(RESPONSE_LOG_FORMAT, 
                method, 
                uri.getPath(),
                exchange.getResponse().getStatusCode()
            );
        });
    }

    private String getFilteredHeaders(ServerHttpRequest request) {
        StringBuilder headers = new StringBuilder("{");
        request.getHeaders().forEach((name, values) -> {
            if (!SENSITIVE_HEADERS.contains(name.toLowerCase())) {
                headers.append("\"").append(name).append("\":");
                headers.append(values.size() == 1 ? 
                    "\"" + values.get(0) + "\"" : 
                    values.toString());
                headers.append(", ");
            }
        });
        if (headers.length() > 1) {
            headers.setLength(headers.length() - 2); // Remove trailing ", "
        }
        return headers.append("}").toString();
    }

    @Override
    public int getOrder() {
        return -1; // High precedence
    }
}
