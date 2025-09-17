package com.crushai.crushai.batch;

import com.crushai.crushai.repository.RefreshRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@Slf4j
public class RefreshTokenCleaner {
    private final RefreshRepository refreshRepository;

    public RefreshTokenCleaner(RefreshRepository refreshRepository) {
        this.refreshRepository = refreshRepository;
    }

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시에 실행
    @Transactional
    public void cleanExpiredTokens() {
        log.info("Starting expired refresh token cleanup task.");
        String now = Instant.now().toString();
        int deletedCount = refreshRepository.deleteByExpirationBefore(now);
        log.info("Finished expired refresh token cleanup task. Deleted tokens: {}", deletedCount);
    }
}
