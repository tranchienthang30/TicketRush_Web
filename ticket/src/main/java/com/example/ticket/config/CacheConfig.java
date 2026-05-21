package com.example.ticket.config;

import com.example.ticket.dto.CategoryEventsResponse;
import com.example.ticket.dto.EventPageResponse;
import com.example.ticket.dto.HomeResponse;
import com.example.ticket.dto.response.EventResponse;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;

@Configuration
public class CacheConfig {
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory, ObjectMapper objectMapper) {
        RedisCacheConfiguration base = RedisCacheConfiguration.defaultCacheConfig()
                .computePrefixWith(cacheName -> "ticketrush:v3:" + cacheName + "::")
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CacheNames.USER_HOME, withTypedValue(base, objectMapper, HomeResponse.class)
                .entryTtl(Duration.ofSeconds(90)));
        cacheConfigs.put(CacheNames.USER_EVENTS_GROUPED, withTypedValue(
                base,
                objectMapper,
                objectMapper.getTypeFactory().constructCollectionType(List.class, CategoryEventsResponse.class))
                .entryTtl(Duration.ofSeconds(90)));
        cacheConfigs.put(CacheNames.USER_EVENTS_SEARCH, withTypedValue(base, objectMapper, EventPageResponse.class)
                .entryTtl(Duration.ofSeconds(45)));
        cacheConfigs.put(CacheNames.USER_EVENT_BY_SLUG, withTypedValue(base, objectMapper, EventResponse.class)
                .entryTtl(Duration.ofSeconds(120)));
        cacheConfigs.put(CacheNames.USER_EVENTS_LEGACY, withTypedValue(
                base,
                objectMapper,
                objectMapper.getTypeFactory().constructCollectionType(List.class, EventResponse.class))
                .entryTtl(Duration.ofSeconds(60)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(base.entryTtl(Duration.ofSeconds(60)))
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    private RedisCacheConfiguration withTypedValue(
            RedisCacheConfiguration base,
            ObjectMapper objectMapper,
            Class<?> targetClass
    ) {
        return withTypedValue(base, objectMapper, objectMapper.getTypeFactory().constructType(targetClass));
    }

    private RedisCacheConfiguration withTypedValue(
            RedisCacheConfiguration base,
            ObjectMapper objectMapper,
            JavaType targetType
    ) {
        Jackson2JsonRedisSerializer<Object> serializer =
                new Jackson2JsonRedisSerializer<>(objectMapper.copy(), targetType);
        return base.serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer));
    }
}
