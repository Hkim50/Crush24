package com.crushai.crushai.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis Geo 기능 전용 설정
 * 사용자 위치 정보 관리를 위한 RedisTemplate 구성
 */
@Configuration
@Slf4j
public class RedisGeoConfig {

    /**
     * Geo 전용 동기 RedisTemplate
     * Bean 이름을 명시하여 다른 RedisTemplate과 충돌 방지
     */
    @Bean("geoRedisTemplate")
    public RedisTemplate<String, String> geoRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Key와 Value 모두 String으로 직렬화
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new StringRedisSerializer());
        
        template.afterPropertiesSet();
        log.info("✅ Redis initialized successfully");
        return template;
    }
}
