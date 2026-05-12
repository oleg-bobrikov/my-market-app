package ru.yandex.practicum.shop.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.lang.NonNull;

import java.time.Duration;

@Slf4j
@Configuration
@EnableCaching
public class RedisConfig implements CachingConfigurer {

    @Bean
    public RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues()
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(RedisSerializer.json())
                );
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {

            @Override
            public void handleCacheGetError(
                    @NonNull RuntimeException exception,
                    @NonNull Cache cache,
                    @NonNull Object key
            ) {
                log.error("Redis cache GET error for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCachePutError(
                    @NonNull RuntimeException exception,
                    @NonNull Cache cache,
                    @NonNull Object key,
                    Object value
            ) {
                log.error("Redis cache PUT error for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(
                    @NonNull RuntimeException exception,
                    @NonNull Cache cache,
                    @NonNull Object key
            ) {
                log.error("Redis cache EVICT error for key {}: {}", key, exception.getMessage());
            }

            @Override
            public void handleCacheClearError(
                    @NonNull RuntimeException exception,
                    @NonNull Cache cache
            ) {
                log.error("Redis cache CLEAR error for cache {}: {}", cache.getName(), exception.getMessage());
            }
        };
    }
}