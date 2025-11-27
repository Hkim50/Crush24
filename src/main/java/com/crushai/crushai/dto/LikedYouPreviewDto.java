package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Liked You 미리보기 DTO (프리 유저용)
 * 
 * 프로필 사진만 포함 (블러 처리용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikedYouPreviewDto {
    
    /**
     * 프로필 사진 URL (첫 번째)
     * 클라이언트에서 블러 처리
     */
    private String profilePhoto;
}
