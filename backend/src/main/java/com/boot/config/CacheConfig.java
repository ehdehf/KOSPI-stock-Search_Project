package com.boot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import java.time.Duration;

@Configuration
public class CacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        
        // KOSPI 데이터는 하루에 한 번만 업데이트되므로 TTL을 24시간으로 설정
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
            // 객체(List<IndexDataDTO>) 저장을 위해 JSON 직렬화 사용
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new GenericJackson2JsonRedisSerializer()))
            // 🌟 캐시 만료 시간을 24시간으로 설정 (스케줄러 업데이트에 의해 무효화될 예정)
            .entryTtl(Duration.ofHours(24)) 
            .disableCachingNullValues(); 

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(config) 
            .build();
    }
}