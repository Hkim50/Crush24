package com.crushai.crushai.repository;

import com.crushai.crushai.entity.UserSwipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSwipeRepository extends JpaRepository<UserSwipe, Long> {
    
    List<UserSwipe> findAllByFromUserId(Long fromUserId);
    
    boolean existsByFromUserIdAndToUserId(Long fromUserId, Long toUserId);
    
    /**
     * 배치 처리를 위한 메서드
     * 특정 사용자가 여러 대상 사용자에게 스와이프한 기록 조회
     * 
     * @param fromUserId 스와이프를 한 사용자 ID
     * @param toUserIds 대상 사용자 ID 목록
     * @return 스와이프 기록 목록
     */
    List<UserSwipe> findByFromUserIdAndToUserIdIn(Long fromUserId, List<Long> toUserIds);
    
    // 유저 삭제 시 사용
    int deleteByFromUserId(Long fromUserId);
    
    int deleteByToUserId(Long toUserId);

}
