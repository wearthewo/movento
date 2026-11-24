package com.movento.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

    @Bean
    public RedisRateLimiter redisRateLimiter() {
        // Default rate limiter: 10 requests per second, burst of 20
        return new RedisRateLimiter(10, 20, 1);
    }

    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // X-Account-Id is only trusted because TrustedHeaderSanitizerFilter strips client copies
            // and AuthenticationFilter sets it from a verified token.
            String accountId = exchange.getRequest().getHeaders().getFirst("X-Account-Id");
            if (accountId != null && !accountId.isEmpty()) {
                return Mono.just("account:" + accountId);
            }
            var remote = exchange.getRequest().getRemoteAddress();
            return Mono.just(remote == null || remote.getAddress() == null ? "anonymous" : "ip:" + remote.getAddress().getHostAddress());
        };
    }
}
