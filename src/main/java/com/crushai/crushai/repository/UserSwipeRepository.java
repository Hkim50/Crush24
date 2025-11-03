package com.crushai.crushai.repository;

import com.crushai.crushai.entity.UserSwipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserSwipeRepository extends JpaRepository<UserSwipe, Long> {
    
    List<UserSwipe> findAllByFromUserId(Long fromUserId);
    
    boolean existsByFromUserIdAndToUserId(Long fromUserId, Long toUserId);
    
    // 유저 삭제 시 사용
    int deleteByFromUserId(Long fromUserId);
    
    int deleteByToUserId(Long toUserId);
}
