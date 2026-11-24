package com.movento.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;

/**
 * Validates the access token and injects trusted identity headers for downstream services.
 *
 * <ul>
 *   <li>{@code optional: false} (default): a valid token is required, otherwise 401.</li>
 *   <li>{@code optional: true}: anonymous requests pass through; a valid token still adds identity
 *   headers (used by the catalog so kids profiles get filtered results).</li>
 * </ul>
 * Inbound copies of the identity headers are always removed by {@link TrustedHeaderSanitizerFilter}.
 */
@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private static final List<String> OPEN_ENDPOINTS = List.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/webhooks/stripe",
            "/actuator/health"
    );

    private final SecretKey signingKey;
    private final String issuer;

    public AuthenticationFilter(@Value("${security.jwt.secret}") String jwtSecret,
                                @Value("${security.jwt.issuer:movento}") String issuer) {
        super(Config.class);
        this.signingKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
        this.issuer = issuer;
    }

    @Override
    public List<String> shortcutFieldOrder() {
        return List.of("optional");
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            if (isOpenEndpoint(request)) {
                return chain.filter(exchange);
            }

            String authHeader = request.getHeaders().getFirst("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return config.isOptional() ? chain.filter(exchange) : reject(exchange, HttpStatus.UNAUTHORIZED);
            }

            Claims claims;
            try {
                claims = Jwts.parserBuilder()
                        .requireIssuer(issuer)
                        .setSigningKey(signingKey)
                        .build()
                        .parseClaimsJws(authHeader.substring(7))
                        .getBody();
            } catch (Exception invalidToken) {
                // An expired token on an optional route is treated as anonymous so public pages keep working.
                return config.isOptional() ? chain.filter(exchange) : reject(exchange, HttpStatus.UNAUTHORIZED);
            }

            List<String> roles = roles(claims);
            if (request.getURI().getPath().startsWith("/api/v1/admin/") && !roles.contains("ROLE_ADMIN")) {
                return reject(exchange, HttpStatus.FORBIDDEN);
            }

            ServerHttpRequest trustedRequest = request.mutate()
                    .headers(headers -> {
                        headers.set("X-Account-Id", String.valueOf(claims.get("accountId")));
                        headers.set("X-User-Email", claims.getSubject());
                        if (!roles.isEmpty()) headers.set("X-User-Roles", String.join(",", roles));
                        if (claims.get("profileId") != null) headers.set("X-Profile-Id", String.valueOf(claims.get("profileId")));
                        if (claims.get("maturity") != null) headers.set("X-Profile-Maturity", String.valueOf(claims.get("maturity")));
                    }).build();
            return chain.filter(exchange.mutate().request(trustedRequest).build());
        };
    }

    private static List<String> roles(Claims claims) {
        Object raw = claims.get("roles");
        if (raw instanceof Collection<?> collection) {
            return collection.stream().map(String::valueOf).toList();
        }
        return raw == null ? List.of() : List.of(String.valueOf(raw));
    }

    private boolean isOpenEndpoint(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return OPEN_ENDPOINTS.stream().anyMatch(path::equals);
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    public static class Config {
        private boolean optional;

        public boolean isOptional() {
            return optional;
        }

        public void setOptional(boolean optional) {
            this.optional = optional;
        }
    }
}
