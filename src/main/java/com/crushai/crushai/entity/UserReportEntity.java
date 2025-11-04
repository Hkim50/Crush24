package com.crushai.crushai.entity;

import com.crushai.crushai.enums.UserReportType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "user_report")
public class UserReportEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private UserEntity reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_user_id", nullable = false)
    private UserEntity reportedUser;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserReportType reason;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant reportedDate;
}
