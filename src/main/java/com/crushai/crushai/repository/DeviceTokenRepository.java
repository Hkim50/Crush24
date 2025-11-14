package com.crushai.crushai.repository;

import com.crushai.crushai.entity.DeviceToken;
import com.crushai.crushai.entity.TokenStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    
    /**
     * 사용자의 활성 토큰 조회
     */
    List<DeviceToken> findByUserIdAndStatus(Long userId, TokenStatus status);
    
    /**
     * 특정 토큰 조회
     */
    Optional<DeviceToken> findByDeviceToken(String deviceToken);
    
    /**
     * 사용자의 모든 토큰 조회
     */
    List<DeviceToken> findByUserId(Long userId);
    
    /**
     * 토큰 존재 여부
     */
    boolean existsByDeviceToken(String deviceToken);
    
    /**
     * 오래된 만료 토큰 정리 (배치용)
     */
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.status = :status AND dt.expiresAt < :before")
    List<DeviceToken> findExpiredTokensBefore(@Param("status") TokenStatus status, @Param("before") Instant before);
    
    /**
     * 장기 미사용 토큰 조회 (배치용)
     */
    @Query("SELECT dt FROM DeviceToken dt WHERE dt.status = :status AND dt.lastUsedAt < :before")
    List<DeviceToken> findInactiveTokens(@Param("status") TokenStatus status, @Param("before") Instant before);
    
    /**
     * 사용자의 특정 상태 토큰 개수 조회
     */
    long countByUserIdAndStatus(Long userId, TokenStatus status);
    
    /**
     * 특정 상태의 토큰 개수 조회 (통계용)
     */
    long countByStatus(TokenStatus status);
}
