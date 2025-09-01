package com.crushai.crushai.service;

import com.crushai.crushai.repository.RefreshRepository;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Instant;

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
