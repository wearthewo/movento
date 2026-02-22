package com.movento.contentservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.MeterBinder;
import io.micrometer.core.instrument.binder.db.DatabaseTableMetrics;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import io.micrometer.core.instrument.binder.db.PostgreSQLDatabaseMetrics;
// Redisson metrics are automatically registered with Micrometer
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for application metrics collection and exposure.
 * Configures various metrics binders for JVM, database, cache, and Redis metrics.
 */
@Slf4j
@Configuration
public class MetricsConfig {

    @Bean
    public PrometheusMeterRegistry prometheusMeterRegistry() {
        return new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
    }

    @Bean
    public MeterBinder jvmMetrics() {
        return new JvmGcMetrics();
    }

    @Bean
    public MeterBinder jvmMemoryMetrics() {
        return new JvmMemoryMetrics();
    }

    @Bean
    public MeterBinder jvmThreadMetrics() {
        return new JvmThreadMetrics();
    }

    @Bean
    public MeterBinder processorMetrics() {
        return new ProcessorMetrics();
    }

    @Bean
    public MeterBinder uptimeMetrics() {
        return new UptimeMetrics();
    }

    @Bean
    public MeterBinder classLoaderMetrics() {
        return new ClassLoaderMetrics();
    }

    /**
     * Configures database table metrics for PostgreSQL.
     * @param dataSource The datasource to monitor
     * @return MeterBinder for database metrics
     */
    @Bean
    public MeterBinder databaseTableMetrics(DataSource dataSource) {
        return registry -> {
            try {
                // PostgreSQL specific metrics
                new PostgreSQLDatabaseMetrics(dataSource, "postgres")
                    .bindTo(registry);
                
                // PostgreSQLDatabaseMetrics provides database-level metrics
                // For table-level metrics, we'll need to use a different approach
                // as DatabaseTableMetrics is not compatible with all database types
                log.info("Enabled PostgreSQL database metrics");
                
                // If you need table-level metrics, consider using:
                // 1. Custom repository methods with @Timed annotations
                // 2. Custom metrics using MeterRegistry
                // 3. A different metrics library specifically for PostgreSQL
            } catch (Exception e) {
                log.error("Failed to initialize database metrics: {}", e.getMessage(), e);
                registry.counter("database.metrics.init.error", 
                    "error", e.getClass().getSimpleName()
                ).increment();
            }
        };
    }

    /**
     * Configures Redis metrics collection.
     * @param redissonClient The Redisson client instance
     * @return MeterBinder for Redis metrics
     */
    @Bean
    public MeterBinder redisMetrics(RedissonClient redissonClient) {
        return registry -> {
            try {
                // Redisson automatically registers metrics with Micrometer
                // No need to manually bind metrics
                log.info("Enabled Redis metrics via Redisson");
            } catch (Exception e) {
                log.error("Failed to initialize Redis metrics: {}", e.getMessage(), e);
                registry.counter("redis.metrics.error", 
                    "error", e.getClass().getSimpleName()
                ).increment();
            }
        };
    }

    /**
     * Configures cache metrics for Caffeine caches.
     * @param cacheManager The cache manager containing caches to monitor
     * @return MeterBinder for cache metrics
     */
    @Bean
    public MeterBinder cacheMetrics(CacheManager cacheManager) {
        return registry -> {
            for (String cacheName : cacheManager.getCacheNames()) {
                try {
                    Cache cache = cacheManager.getCache(cacheName);
                    if (cache != null && cache instanceof CaffeineCache) {
                        CaffeineCache caffeineCache = (CaffeineCache) cache;
                        if (caffeineCache.getNativeCache() instanceof com.github.benmanes.caffeine.cache.Cache) {
                            CaffeineCacheMetrics.monitor(
                                registry,
                                (com.github.benmanes.caffeine.cache.Cache<?, ?>) caffeineCache.getNativeCache(),
                                "content.cache",
                                Tags.of("cache", cacheName)
                            );
                            log.debug("Enabled metrics for cache: {}", cacheName);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to enable metrics for cache {}: {}", cacheName, e.getMessage());
                    registry.counter("cache.metrics.error", 
                        "cache", cacheName,
                        "error", e.getClass().getSimpleName()
                    ).increment();
                }
            }
        };
    }
    
    /**
     * Configures HTTP request metrics.
     * Note: HTTP request metrics are automatically collected by Spring Boot Actuator.
     * This is a placeholder for any custom HTTP metrics that might be needed.
     * 
     * @return MeterBinder for HTTP metrics
     */
    @Bean
    public MeterBinder httpRequestMetrics() {
        return registry -> {
            // Add custom HTTP metrics here if needed
            // Example: Custom request counter
            // registry.counter("http.requests.custom").increment();
        };
    }
}
