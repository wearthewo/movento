package com.movento.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import java.nio.charset.StandardCharsets;


@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final List<String> openApiEndpoints = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/webhooks/stripe",
            "/api/v1/catalog",
            "/actuator/health"
    );

    @Value("${security.jwt.secret}")
    private String jwtSecret;

    @Value("${security.jwt.issuer:movento}")
    private String issuer;

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

            try {
                String token = authHeader.substring(7);
                Claims claims = Jwts.parserBuilder()
                        .requireIssuer(issuer)
                        .setSigningKey(Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)))
                        .build().parseClaimsJws(token).getBody();
                if (request.getURI().getPath().startsWith("/api/v1/admin/") && !String.valueOf(claims.get("roles")).contains("ROLE_ADMIN")) {
                    exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                    return exchange.getResponse().setComplete();
                }
                ServerHttpRequest trustedRequest = request.mutate()
                        .headers(headers -> {
                            headers.remove("X-Account-Id"); headers.remove("X-Profile-Id"); headers.remove("X-User-Email"); headers.remove("X-User-Roles");
                            headers.add("X-Account-Id", String.valueOf(claims.get("accountId")));
                            headers.add("X-User-Email", claims.getSubject());
                            if (claims.get("profileId") != null) headers.add("X-Profile-Id", String.valueOf(claims.get("profileId")));
                            Object roles = claims.get("roles");
                            if (roles != null) headers.add("X-User-Roles", roles.toString());
                        }).build();
                return chain.filter(exchange.mutate().request(trustedRequest).build());
            } catch (Exception invalidToken) {
                return handleUnAuthorized(exchange);
            }
        };
    }

    private boolean isOpenEndpoint(ServerHttpRequest request) {
        return openApiEndpoints.stream()
                .anyMatch(uri -> request.getURI().getPath().startsWith(uri));
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
