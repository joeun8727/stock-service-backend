package com.stocknews.api.common.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisConfig {

    @Value("${cache.ttl.stock-profile}") private long stockProfileTtl;
    @Value("${cache.ttl.stock-metrics}") private long stockMetricsTtl;
    @Value("${cache.ttl.stock-news}") private long stockNewsTtl;
    @Value("${cache.ttl.stock-summary}") private long stockSummaryTtl;
    @Value("${cache.ttl.sector-ranking}") private long sectorRankingTtl;
    @Value("${cache.ttl.sector-largecap}") private long sectorLargecapTtl;
    @Value("${cache.ttl.sector-growth}") private long sectorGrowthTtl;
    @Value("${cache.ttl.macro}") private long macroTtl;

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory factory) {
        GenericJackson2JsonRedisSerializer serializer =
                new GenericJackson2JsonRedisSerializer(redisObjectMapper());

        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues();

        Map<String, RedisCacheConfiguration> cacheConfigs = new HashMap<>();
        cacheConfigs.put(CacheKeys.STOCK_PROFILE,         defaultConfig.entryTtl(Duration.ofSeconds(stockProfileTtl)));
        cacheConfigs.put(CacheKeys.STOCK_METRICS,         defaultConfig.entryTtl(Duration.ofSeconds(stockMetricsTtl)));
        cacheConfigs.put(CacheKeys.STOCK_NEWS,            defaultConfig.entryTtl(Duration.ofSeconds(stockNewsTtl)));
        cacheConfigs.put(CacheKeys.STOCK_SUMMARY,         defaultConfig.entryTtl(Duration.ofSeconds(stockSummaryTtl)));
        cacheConfigs.put(CacheKeys.STOCK_SCORE_BREAKDOWN, defaultConfig.entryTtl(Duration.ofSeconds(stockProfileTtl)));   // 6h — 배치 주기와 동일
        cacheConfigs.put(CacheKeys.STOCK_SENTIMENT_TREND, defaultConfig.entryTtl(Duration.ofSeconds(stockMetricsTtl)));  // 1h — 뉴스 수집 주기와 동일
        cacheConfigs.put(CacheKeys.SECTOR_RANKING,        defaultConfig.entryTtl(Duration.ofSeconds(sectorRankingTtl)));
        cacheConfigs.put(CacheKeys.SECTOR_LARGECAP,       defaultConfig.entryTtl(Duration.ofSeconds(sectorLargecapTtl)));
        cacheConfigs.put(CacheKeys.SECTOR_GROWTH,         defaultConfig.entryTtl(Duration.ofSeconds(sectorGrowthTtl)));
        cacheConfigs.put(CacheKeys.SECTOR_RULE_OF_40,     defaultConfig.entryTtl(Duration.ofSeconds(sectorLargecapTtl))); // 6h — 배치 주기와 동일
        cacheConfigs.put(CacheKeys.SECTOR_VALUATION,      defaultConfig.entryTtl(Duration.ofSeconds(sectorLargecapTtl))); // 6h — 배치 주기와 동일
        cacheConfigs.put(CacheKeys.TOP_NEWS,              defaultConfig.entryTtl(Duration.ofSeconds(stockNewsTtl)));      // 30min — 뉴스 수집 주기와 동일
        cacheConfigs.put(CacheKeys.MACRO,                 defaultConfig.entryTtl(Duration.ofSeconds(macroTtl)));

        return RedisCacheManager.builder(factory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigs)
                .build();
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper mapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        // @class 타입 정보 포함 필수.
        // NON_FINAL 은 Java record(final 클래스)를 제외하므로 EVERYTHING 사용.
        mapper.activateDefaultTyping(
                BasicPolymorphicTypeValidator.builder()
                        .allowIfBaseType(Object.class)
                        .build(),
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}
