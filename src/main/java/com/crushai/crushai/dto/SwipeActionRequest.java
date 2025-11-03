package com.crushai.crushai.dto;

import com.crushai.crushai.enums.SwipeType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SwipeActionRequest {
    
    @NotNull(message = "Target user ID is required")
    private Long targetUserId;
    
    @NotNull(message = "Swipe action is required")
    private SwipeType action;
}
