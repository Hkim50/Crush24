package com.crushai.crushai.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

/**
 * 사용자 차단 엔티티
 * 특정 사용자를 차단한 기록을 저장
 */
@Entity
@Table(name = "user_block", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_id", "blocked_user_id"}))
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBlock {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * 차단한 사용자 ID
     */
    @Column(name = "blocker_id", nullable = false)
    private Long blockerId;
    
    /**
     * 차단당한 사용자 ID
     */
    @Column(name = "blocked_user_id", nullable = false)
    private Long blockedUserId;
    
    /**
     * 차단 시각
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;
}
