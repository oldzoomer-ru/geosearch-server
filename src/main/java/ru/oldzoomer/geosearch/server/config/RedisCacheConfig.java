package ru.oldzoomer.geosearch.server.config;

import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.Map;

/**
 * Конфигурация кэширования на базе Redis.
 * <p>
 * Настраивает три кэша с разными TTL:
 * <ul>
 *   <li>{@code transport-stops} — 30 минут</li>
 *   <li>{@code poi} — 30 минут</li>
 *   <li>{@code routes} — 1 час</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class RedisCacheConfig {

    private static final Duration TRANSPORT_STOPS_TTL = Duration.ofMinutes(30);
    private static final Duration POI_TTL = Duration.ofMinutes(30);
    private static final Duration ROUTES_TTL = Duration.ofHours(1);

    @Bean RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        var baseConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(GenericJacksonJsonRedisSerializer
                        .builder()
                        .enableSpringCacheNullValueSupport()
                        .build()));

        var cacheConfigurations = Map.of(
                "transport-stops", baseConfig.entryTtl(TRANSPORT_STOPS_TTL),
                "poi", baseConfig.entryTtl(POI_TTL),
                "routes", baseConfig.entryTtl(ROUTES_TTL)
        );

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(baseConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
