package com.crushai.crushai.dto;

import com.crushai.crushai.enums.SwipeType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Liked You 액션 요청 DTO
 * 
 * Likes You 페이지에서 LIKE 또는 PASS
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LikedYouActionRequest {
    
    @NotNull(message = "Target user ID is required")
    private Long targetUserId;
    
    @NotNull(message = "Action is required")
    private SwipeType action;  // LIKE or PASS
}
