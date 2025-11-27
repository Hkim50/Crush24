package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Liked You 미리보기 응답 DTO (프리 유저용)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikedYouPreviewResponse {
    
    /**
     * 프로필 사진 목록 (블러 처리용)
     */
    private List<LikedYouPreviewDto> likes;
    
    /**
     * 전체 좋아요 개수
     */
    private Integer totalCount;

}
