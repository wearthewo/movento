package com.movento.contentservice.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.redisson.spring.cache.CacheConfig;
import org.redisson.spring.cache.RedissonSpringCacheManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
              .setAddress("redis://" + redisHost + ":" + redisPort)
              .setConnectionPoolSize(10)
              .setConnectionMinimumIdleSize(5)
              .setIdleConnectionTimeout(10000);
        return Redisson.create(config);
    }

    @Bean
    public CacheManager cacheManager(RedissonClient redissonClient) {
        Map<String, CacheConfig> config = new HashMap<>();
        // 1 hour cache for content
        config.put("contentCache", new CacheConfig(60 * 60 * 1000, 30 * 60 * 1000));
        // 24 hours cache for genres
        config.put("genreCache", new CacheConfig(24 * 60 * 60 * 1000, 12 * 60 * 60 * 1000));
        
        return new RedissonSpringCacheManager(redissonClient, config);
    }
}
