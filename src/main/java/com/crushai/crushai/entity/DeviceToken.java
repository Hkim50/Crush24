package com.crushai.crushai.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "device_tokens", 
       indexes = {
           @Index(name = "idx_userId", columnList = "userId"),
           @Index(name = "idx_deviceToken", columnList = "deviceToken"),
           @Index(name = "idx_status", columnList = "status")
       })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EntityListeners(AuditingEntityListener.class)
public class DeviceToken {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false, unique = true, length = 200)
    private String deviceToken;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeviceType deviceType;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TokenStatus status = TokenStatus.ACTIVE;
    
    @Column(length = 100)
    private String deviceModel;
    
    @Column(length = 50)
    private String osVersion;
    
    @Column(length = 50)
    private String appVersion;
    
    private Instant lastUsedAt;
    
    private Instant expiresAt;
    
    @Column(columnDefinition = "TEXT")
    private String failureReason;
    
    @Builder.Default
    private Integer failureCount = 0;
    
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
    
    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;
    
    /**
     * 토큰 활성화
     */
    public void activate() {
        this.status = TokenStatus.ACTIVE;
        this.failureCount = 0;
        this.failureReason = null;
    }
    
    /**
     * 토큰 사용 기록
     */
    public void markAsUsed() {
        this.lastUsedAt = Instant.now();
        this.failureCount = 0;
    }
    
    /**
     * 실패 기록
     */
    public void recordFailure(String reason) {
        this.failureCount = (this.failureCount == null ? 0 : this.failureCount) + 1;
        this.failureReason = reason;
        
        // 3번 연속 실패 시 INVALID 처리
        if (this.failureCount >= 3) {
            this.status = TokenStatus.INVALID;
        }
    }
    
    /**
     * 만료 처리
     */
    public void expire() {
        this.status = TokenStatus.EXPIRED;
        this.expiresAt = Instant.now();
    }
    
    /**
     * 디바이스 정보 업데이트
     */
    public void updateDeviceInfo(String deviceModel, String osVersion, String appVersion) {
        this.deviceModel = deviceModel;
        this.osVersion = osVersion;
        this.appVersion = appVersion;
    }
}
