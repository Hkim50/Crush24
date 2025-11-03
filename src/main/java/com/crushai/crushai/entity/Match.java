package com.crushai.crushai.entity;

import com.crushai.crushai.enums.MatchType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "matches",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user1_id", "user2_id"})
)
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private Long user1Id;

    @Column(name = "user2_id", nullable = false)
    private Long user2Id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MatchType matchType;

    @Column(name = "chat_room_id")
    private String chatRoomId;

    @Column(nullable = false)
    private boolean isActive = true;

    @Column(nullable = false)
    private LocalDateTime matchedAt;

    @PrePersist
    protected void onCreate() {
        matchedAt = LocalDateTime.now();
    }

    public void setChatRoomId(String chatRoomId) {
        this.chatRoomId = chatRoomId;
    }
    
    /**
     * 매칭 비활성화
     * 한쪽 유저가 삭제될 때 사용
     */
    public void deactivate() {
        this.isActive = false;
    }
}
