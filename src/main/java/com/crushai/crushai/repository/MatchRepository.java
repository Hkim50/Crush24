package com.crushai.crushai.repository;

import com.crushai.crushai.entity.Match;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MatchRepository extends JpaRepository<Match, Long> {
    
    @Query("SELECT m FROM Match m WHERE (m.user1Id = :userId OR m.user2Id = :userId) AND m.isActive = true")
    List<Match> findActiveMatchesByUserId(@Param("userId") Long userId);
    
    // 특정 유저가 포함된 모든 매칭 조회
    @Query("SELECT m FROM Match m WHERE m.user1Id = :userId OR m.user2Id = :userId")
    List<Match> findAllMatchesByUserId(@Param("userId") Long userId);
    
    // 삭제될 유저가 포함된 매칭만 조회 (양쪽 또는 한쪽)
    @Query("SELECT m FROM Match m WHERE m.user1Id IN :deletedUserIds OR m.user2Id IN :deletedUserIds")
    List<Match> findMatchesWithDeletedUsers(@Param("deletedUserIds") List<Long> deletedUserIds);
}
