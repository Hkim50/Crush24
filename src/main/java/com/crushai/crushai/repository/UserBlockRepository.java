package com.crushai.crushai.repository;

import com.crushai.crushai.entity.UserBlock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserBlockRepository extends JpaRepository<UserBlock, Long> {
    
    /**
     * 특정 사용자가 차단한 사용자 ID 목록
     */
    @Query("SELECT ub.blockedUserId FROM UserBlock ub WHERE ub.blockerId = :userId")
    List<Long> findBlockedUserIdsByBlockerId(@Param("userId") Long userId);
    
    /**
     * 특정 사용자를 차단한 사용자 ID 목록 (나를 차단한 사람들)
     */
    @Query("SELECT ub.blockerId FROM UserBlock ub WHERE ub.blockedUserId = :userId")
    List<Long> findBlockerIdsByBlockedUserId(@Param("userId") Long userId);
    
    /**
     * 이미 차단했는지 확인
     */
    boolean existsByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);
    
    /**
     * 차단 해제
     */
    void deleteByBlockerIdAndBlockedUserId(Long blockerId, Long blockedUserId);
}
