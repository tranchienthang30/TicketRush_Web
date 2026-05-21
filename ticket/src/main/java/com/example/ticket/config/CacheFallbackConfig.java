package com.example.ticket.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CacheFallbackConfig {
    private static final Logger log = LoggerFactory.getLogger(CacheFallbackConfig.class);

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache GET error on cache='{}' key='{}': {}", cacheName(cache), key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Cache PUT error on cache='{}' key='{}': {}", cacheName(cache), key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Cache EVICT error on cache='{}' key='{}': {}", cacheName(cache), key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Cache CLEAR error on cache='{}': {}", cacheName(cache), exception.getMessage());
            }

            private String cacheName(Cache cache) {
                return cache == null ? "unknown" : cache.getName();
            }
        };
    }
}
