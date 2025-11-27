package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Liked You 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LikedYouResponse {
    
    private List<LikedYouDto> likes;
    private Integer totalCount;
}
