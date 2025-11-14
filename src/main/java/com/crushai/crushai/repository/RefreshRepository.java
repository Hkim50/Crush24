package com.crushai.crushai.repository;

import com.crushai.crushai.entity.RefreshEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface RefreshRepository extends JpaRepository<RefreshEntity, Long> {
    
    /**
     * 토큰 해시로 조회
     */
    Optional<RefreshEntity> findByTokenHash(String tokenHash);
    
    /**
     * 토큰 해시 존재 여부 확인
     */
    boolean existsByTokenHash(String tokenHash);
    
    /**
     * 토큰 해시로 삭제
     */
    @Transactional
    @Modifying
    void deleteByTokenHash(String tokenHash);
    
    /**
     * 만료된 토큰 삭제 (배치용)
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM RefreshEntity r WHERE r.expiresAt < :now")
    int deleteByExpiresAtBefore(@Param("now") Instant now);
    
    /**
     * 특정 사용자의 모든 토큰 삭제 (전체 로그아웃)
     */
    @Transactional
    @Modifying
    void deleteAllByUser_Id(Long userId);
    
    /**
     * 특정 사용자의 특정 디바이스 토큰 삭제 (디바이스별 로그아웃)
     */
    @Transactional
    @Modifying
    void deleteByUser_IdAndDeviceId(Long userId, String deviceId);
    
    /**
     * 사용자의 활성 토큰 목록 조회
     */
    @Query("SELECT r FROM RefreshEntity r WHERE r.user.id = :userId AND r.expiresAt > :now")
    List<RefreshEntity> findActiveTokensByUserId(@Param("userId") Long userId, @Param("now") Instant now);
    
    /**
     * 사용자의 토큰 개수 조회
     */
    long countByUser_Id(Long userId);
    
    /**
     * 특정 시간 이전에 생성된 만료 토큰 조회 (배치 정리용)
     */
    @Query("SELECT r FROM RefreshEntity r WHERE r.expiresAt < :expiryThreshold")
    List<RefreshEntity> findExpiredTokens(@Param("expiryThreshold") Instant expiryThreshold);
}
