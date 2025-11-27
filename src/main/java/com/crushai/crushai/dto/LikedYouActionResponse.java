package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Liked You 액션 응답 DTO
 * 
 * LIKE 또는 PASS 액션 결과
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikedYouActionResponse {
    
    // 매칭 여부
    private Boolean isMatch;
    
    // 채팅방 ID (매칭된 경우만)
    private String chatRoomId;
    
    // 상대방 유저 정보 (매칭된 경우만)
    private MatchedUserDto otherUser;
    
    // 액션 메시지
    private String message;
    
    /**
     * PASS 응답 생성
     */
    public static LikedYouActionResponse passed() {
        return LikedYouActionResponse.builder()
                .isMatch(false)
                .message("User passed")
                .build();
    }
    
    /**
     * LIKE 응답 생성 (매칭 안됨)
     */
    public static LikedYouActionResponse likedWithoutMatch() {
        return LikedYouActionResponse.builder()
                .isMatch(false)
                .message("User liked")
                .build();
    }
    
    /**
     * LIKE 응답 생성 (매칭됨)
     */
    public static LikedYouActionResponse matched(String chatRoomId, MatchedUserDto otherUser) {
        return LikedYouActionResponse.builder()
                .isMatch(true)
                .chatRoomId(chatRoomId)
                .otherUser(otherUser)
                .message("It's a match!")
                .build();
    }
}
