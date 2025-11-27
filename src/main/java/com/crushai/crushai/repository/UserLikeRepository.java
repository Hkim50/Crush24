package com.crushai.crushai.repository;

import com.crushai.crushai.entity.UserLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserLikeRepository extends JpaRepository<UserLike, Long> {
    
    boolean existsByFromUserIdAndToUserId(Long fromUserId, Long toUserId);
    
    @Query("SELECT l FROM UserLike l WHERE l.fromUserId IN :fromUserIds AND l.toUserId = :toUserId")
    List<UserLike> findAllByFromUserIdInAndToUserId(
        @Param("fromUserIds") List<Long> fromUserIds,
        @Param("toUserId") Long toUserId
    );
    
    /**
     * 나를 좋아한 유저 목록 조회 (최신순)
     */
    List<UserLike> findAllByToUserIdOrderByCreatedAtDesc(Long toUserId);
    
    // 유저 삭제 시 사용
    int deleteByFromUserId(Long fromUserId);
    
    int deleteByToUserId(Long toUserId);
}
