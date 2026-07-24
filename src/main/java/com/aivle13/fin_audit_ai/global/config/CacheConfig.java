package com.aivle13.fin_audit_ai.global.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

import java.time.Duration;
import java.util.Map;

@EnableCaching
@Configuration
public class CacheConfig {

    public static final String EXPLAINABILITY_CACHE = "explainability";
    public static final String FAIRNESS_CACHE = "fairness";
    public static final String DATASETS_CACHE = "datasets";

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    private static final PolymorphicTypeValidator CACHE_TYPE_VALIDATOR = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.aivle13.fin_audit_ai")
            .allowIfSubType("java.util")
            .build();

    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(DEFAULT_TTL)
                .disableCachingNullValues()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        new StringRedisSerializer()
                ))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(
                        GenericJacksonJsonRedisSerializer.builder()
                                .enableDefaultTyping(CACHE_TYPE_VALIDATOR)
                                .build()
                ));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(Map.of(
                        EXPLAINABILITY_CACHE, defaultConfig,
                        FAIRNESS_CACHE, defaultConfig,
                        DATASETS_CACHE, defaultConfig
                ))
                .build();
    }
}
