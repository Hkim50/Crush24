package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 나를 좋아한 유저 정보 DTO
 * 
 * 범블 스타일: Liked You 화면
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikedYouDto {
    
    // 좋아요 정보
    private Long likeId;
    private LocalDateTime likedAt;
    
    // 유저 기본 정보
    private Long userId;
    private String name;
    private Integer age;
    private String profilePhoto;  // 첫 번째 프로필 사진
    
    // 위치 정보
    private String locationName;
}
