package com.crushai.crushai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

/**
 * Refresh Token 엔티티
 * JWT Refresh Token의 해시값과 메타데이터를 저장
 */
@Entity
@Table(name = "refresh_tokens",
    indexes = {
        @Index(name = "idx_token_hash", columnList = "tokenHash"),
        @Index(name = "idx_user_id", columnList = "userId"),
        @Index(name = "idx_expires_at", columnList = "expiresAt"),
        @Index(name = "idx_device_id", columnList = "deviceId")
    }
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class RefreshEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 사용자 ID (외래키)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_token_user"))
    private UserEntity user;
    
    /**
     * Refresh Token의 SHA-256 해시값
     * 실제 토큰 대신 해시를 저장하여 보안 강화
     */
    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;
    
    /**
     * 디바이스 고유 ID
     * 멀티 디바이스 지원을 위한 식별자
     */
    @Column(nullable = false, length = 100)
    private String deviceId;
    
    /**
     * 디바이스 타입
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private DeviceType deviceType;
    
    /**
     * 디바이스 이름 (선택적)
     * 예: "iPhone 14 Pro", "Galaxy S23"
     */
    @Column(length = 100)
    private String deviceName;
    
    /**
     * 토큰 만료 시간
     */
    @Column(nullable = false)
    private Instant expiresAt;
    
    /**
     * 마지막 사용 시간
     */
    private Instant lastUsedAt;
    
    /**
     * 토큰 생성 시간
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    /**
     * 마지막 사용 시간 갱신
     */
    public void updateLastUsed() {
        this.lastUsedAt = Instant.now();
    }
    
    /**
     * 토큰이 만료되었는지 확인
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}