package com.crushai.crushai.batch;

import com.crushai.crushai.repository.RefreshRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Refresh Token 정리 배치 작업
 * 만료된 토큰을 주기적으로 삭제하여 DB 공간 확보
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenCleaner {
    
    private final RefreshRepository refreshRepository;

    /**
     * 매일 새벽 3시(UTC)에 만료된 Refresh Token 삭제
     * 
     * 배치 크기를 제한하여 대량 삭제 시 DB 부하 방지
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "UTC")
    @Transactional
    public void cleanExpiredTokens() {
        log.info("Starting expired refresh token cleanup task");
        
        try {
            Instant now = Instant.now();
            int deletedCount = refreshRepository.deleteByExpiresAtBefore(now);
            
            log.info("Expired refresh token cleanup completed. Deleted tokens: {}", deletedCount);
            
            // 통계 로깅
            long totalTokens = refreshRepository.count();
            log.info("Total active refresh tokens: {}", totalTokens);
            
        } catch (Exception e) {
            log.error("Failed to cleanup expired refresh tokens", e);
        }
    }
}
