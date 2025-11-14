package com.crushai.crushai.scheduler;

import com.crushai.crushai.entity.DeviceToken;
import com.crushai.crushai.entity.TokenStatus;
import com.crushai.crushai.repository.DeviceTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 디바이스 토큰 정리 스케줄러
 * 만료되거나 오래 사용하지 않은 토큰을 주기적으로 정리
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DeviceTokenCleanupScheduler {
    
    private final DeviceTokenRepository deviceTokenRepository;
    
    /**
     * 매일 새벽 3시에 오래된 토큰 정리
     * 
     * - 30일 이상 된 만료/무효 토큰 삭제
     * - 90일 이상 미사용 토큰 만료 처리
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        log.info("Starting device token cleanup job...");
        
        try {
            // 1. 30일 이상 된 만료 토큰 삭제
            Instant thirtyDaysAgo = Instant.now().minus(30, ChronoUnit.DAYS);
            
            List<DeviceToken> expiredTokens = deviceTokenRepository
                .findExpiredTokensBefore(TokenStatus.EXPIRED, thirtyDaysAgo);
            
            if (!expiredTokens.isEmpty()) {
                deviceTokenRepository.deleteAll(expiredTokens);
                log.info("✅ Deleted {} expired tokens (older than 30 days)", expiredTokens.size());
            }
            
            // 2. 30일 이상 된 무효 토큰 삭제
            List<DeviceToken> invalidTokens = deviceTokenRepository
                .findExpiredTokensBefore(TokenStatus.INVALID, thirtyDaysAgo);
            
            if (!invalidTokens.isEmpty()) {
                deviceTokenRepository.deleteAll(invalidTokens);
                log.info("✅ Deleted {} invalid tokens (older than 30 days)", invalidTokens.size());
            }
            
            // 3. 90일 이상 미사용 활성 토큰 만료 처리
            Instant ninetyDaysAgo = Instant.now().minus(90, ChronoUnit.DAYS);
            
            List<DeviceToken> inactiveTokens = deviceTokenRepository
                .findInactiveTokens(TokenStatus.ACTIVE, ninetyDaysAgo);
            
            if (!inactiveTokens.isEmpty()) {
                inactiveTokens.forEach(DeviceToken::expire);
                log.info("✅ Expired {} inactive tokens (no use for 90+ days)", inactiveTokens.size());
            }
            
            log.info("Device token cleanup job completed successfully");
            
        } catch (Exception e) {
            log.error("❌ Error during device token cleanup", e);
        }
    }
    
    /**
     * 매주 월요일 오전 2시에 토큰 통계 로깅
     */
    @Scheduled(cron = "0 0 2 * * MON")
    @Transactional(readOnly = true)
    public void logTokenStatistics() {
        try {
            long activeCount = deviceTokenRepository.countByStatus(TokenStatus.ACTIVE);
            long expiredCount = deviceTokenRepository.countByStatus(TokenStatus.EXPIRED);
            long invalidCount = deviceTokenRepository.countByStatus(TokenStatus.INVALID);
            long totalCount = deviceTokenRepository.count();
            
            log.info("📊 Device Token Statistics:");
            log.info("  - Active: {}", activeCount);
            log.info("  - Expired: {}", expiredCount);
            log.info("  - Invalid: {}", invalidCount);
            log.info("  - Total: {}", totalCount);
            
        } catch (Exception e) {
            log.error("❌ Error logging token statistics", e);
        }
    }
}
