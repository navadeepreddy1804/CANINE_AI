package com.canineai.backend.config.cache;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        // ConcurrentMapCacheManager is the standard fallback when Redis is not active.
        // It provides thread-safe in-memory caching that mirrors the same interfaces as Redis.
        return new ConcurrentMapCacheManager("patients", "studies", "reports");
    }
}
