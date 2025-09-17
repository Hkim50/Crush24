package com.crushai.crushai.batch;

import com.crushai.crushai.repository.RefreshRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class RefreshTokenCleaner {
    private final RefreshRepository refreshRepository;

    public RefreshTokenCleaner(RefreshRepository refreshRepository) {
        this.refreshRepository = refreshRepository;
    }

    @Scheduled(cron = "0 0 3 * * *") // 매일 새벽 3시 실행
    public void cleanExpiredTokens() {
        String now = Instant.now().toString();
        refreshRepository.deleteByExpirationBefore(now);
    }
}
