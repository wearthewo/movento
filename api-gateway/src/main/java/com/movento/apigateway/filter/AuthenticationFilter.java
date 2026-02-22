package com.movento.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;


@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final List<String> openApiEndpoints = List.of(
            "/api/users/register",
            "/api/users/login",
            "/api/users/refresh-token",
            "/actuator/health"
    );

    public AuthenticationFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // Skip authentication for open endpoints
            if (isOpenEndpoint(request)) {
                return chain.filter(exchange);
            }

            // Check for Authorization header
            if (!request.getHeaders().containsKey("Authorization")) {
                return handleUnAuthorized(exchange);
            }

            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return handleUnAuthorized(exchange);
            }

            // TODO: Validate JWT token with User Service
            // For now, just pass the request through
            return chain.filter(exchange);
        };
    }

    private boolean isOpenEndpoint(ServerHttpRequest request) {
        return openApiEndpoints.stream()
                .anyMatch(uri -> request.getURI().getPath().contains(uri));
    }

    private Mono<Void> handleUnAuthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        // Configuration properties if needed
    }

    // DTO for user info response from user service
//    private static class UserInfoResponse {
//        private String id;
//        private String email;
//        private List<String> roles;
//
//        // Getters and setters
//        public String getId() { return id; }
//        public void setId(String id) { this.id = id; }
//
//        public String getEmail() { return email; }
//        public void setEmail(String email) { this.email = email; }
//
//        public List<String> getRoles() { return roles; }
//        public void setRoles(List<String> roles) { this.roles = roles; }
//    }
}
