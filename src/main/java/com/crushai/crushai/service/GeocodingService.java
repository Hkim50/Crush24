package com.crushai.crushai.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 좌표 → 위치명 변환 서비스 (캐싱 포함)
 */
@Service
@Slf4j
public class GeocodingService {

    private final NominatimService nominatimService;
    private final RedisTemplate<String, String> redisTemplate;

    private static final double CACHE_ROUNDING_DEGREE = 0.05; // 약 5km
    private static final int CACHE_TTL_DAYS = 7;
    private static final String CACHE_KEY_PREFIX = "location:";

    public GeocodingService(NominatimService nominatimService,
                           @Qualifier("geoRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.nominatimService = nominatimService;
        this.redisTemplate = redisTemplate;
    }

    /**
     * 좌표를 위치명으로 변환 (캐싱 적용)
     * 
     * 1. 좌표를 0.05도 단위로 반올림 (약 5km 그리드)
     * 2. 캐시 확인
     * 3. 캐시 미스 시 Nominatim API 호출 후 캐싱
     * 
     * @param latitude 위도
     * @param longitude 경도
     * @return "Los Angeles, CA" 형식의 위치명, 실패 시 null
     */
    public String getLocationName(double latitude, double longitude) {
        // 1. 좌표 반올림 (0.05도 단위)
        double roundedLat = roundCoordinate(latitude);
        double roundedLon = roundCoordinate(longitude);
        
        String cacheKey = buildCacheKey(roundedLat, roundedLon);
        
        // 2. 캐시 확인
        try {
            String cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                log.debug("Cache hit for location: {} → {}", cacheKey, cached);
                return cached;
            }
        } catch (Exception e) {
            log.warn("Failed to get from cache: {}", e.getMessage());
        }
        
        // 3. 캐시 미스 → Nominatim API 호출
        log.debug("Cache miss for location: {}", cacheKey);
        String locationName = nominatimService.getLocationName(latitude, longitude);
        
        if (locationName != null) {
            // 4. 캐시 저장 (7일 TTL)
            try {
                redisTemplate.opsForValue().set(
                    cacheKey,
                    locationName,
                    CACHE_TTL_DAYS,
                    TimeUnit.DAYS
                );
                log.info("Cached location: {} → {}", cacheKey, locationName);
            } catch (Exception e) {
                log.warn("Failed to save to cache: {}", e.getMessage());
            }
        }
        
        return locationName;
    }

    /**
     * 좌표를 0.05도 단위로 반올림
     * 
     * @param coordinate 위도 또는 경도
     * @return 반올림된 좌표
     */
    private double roundCoordinate(double coordinate) {
        double divisor = 1.0 / CACHE_ROUNDING_DEGREE;
        return Math.round(coordinate * divisor) / divisor;
    }

    /**
     * 캐시 키 생성
     * 
     * @param latitude 반올림된 위도
     * @param longitude 반올림된 경도
     * @return 캐시 키 (예: "location:37.50_127.05")
     */
    private String buildCacheKey(double latitude, double longitude) {
        return String.format("%s%.2f_%.2f", CACHE_KEY_PREFIX, latitude, longitude);
    }
}
