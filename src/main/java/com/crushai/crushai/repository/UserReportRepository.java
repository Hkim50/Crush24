package com.crushai.crushai.repository;

import com.crushai.crushai.entity.UserReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserReportRepository extends JpaRepository<UserReportEntity, Long> {
    
    /**
     * 특정 신고자가 특정 사용자를 이미 신고했는지 확인
     */
    boolean existsByReporterIdAndReportedUserId(Long reporterId, Long reportedUserId);
}
