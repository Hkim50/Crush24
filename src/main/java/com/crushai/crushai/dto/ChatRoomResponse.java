package com.crushai.crushai.dto;

import com.crushai.crushai.enums.MatchType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

/**
 * 채팅방 정보 응답 DTO
 * 
 * 범블/틴더 스타일: 상대방 프로필 + 마지막 메시지
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRoomResponse {
    
    // 채팅방 기본 정보
    private String chatRoomId;
    private Long matchId;
    private MatchType matchType;
    private LocalDateTime matchedAt;
    
    // 상대방 정보
    private OtherUserInfo otherUser;
    
    // 마지막 메시지 정보
    private String lastMessage;
    private Instant lastMessageAt;
    private Integer unreadCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OtherUserInfo {
        private Long userId;
        private String name;
        private Integer age;
        private String profilePhoto;  // 첫 번째 프로필 사진
    }
}
