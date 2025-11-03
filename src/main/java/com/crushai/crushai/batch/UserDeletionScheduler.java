package com.crushai.crushai.batch;

import com.crushai.crushai.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Component
@Slf4j
public class UserDeletionScheduler {

    private final UserService userService;

    public UserDeletionScheduler(UserService userService) {
        this.userService = userService;
    }

    // 매일 새벽 1시에 실행
//    @Scheduled(cron = "0 0 1 * * *", zone = "UTC")

    // 매일 UTC 오전 10시 40분 0초부터 59초까지, 1초마다 실행
    @Scheduled(cron = "0 0 2 * * *", zone = "UTC")
    public void deleteExpiredUsers() {
        log.info("qqwwnejfonqerjfnqejrinf");
        log.info("🔥 스케줄러 실행됨!");
        Instant now = Instant.now();
        userService.deleteExpiredUsers(now);
    }
}
