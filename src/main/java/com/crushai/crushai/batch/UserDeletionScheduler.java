package com.crushai.crushai.batch;

import com.crushai.crushai.service.UserService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
public class UserDeletionScheduler {

    private final UserService userService;

    public UserDeletionScheduler(UserService userService) {
        this.userService = userService;
    }

    // 매일 새벽 1시에 실행
    @Scheduled(cron = "0 0 1 * * *", zone = "UTC")
    public void deleteExpiredUsers() {
        Instant now = Instant.now();
        userService.deleteExpiredUsers(now);
    }
}
