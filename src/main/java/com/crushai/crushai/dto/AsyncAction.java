package com.crushai.crushai.dto;

import com.crushai.crushai.enums.SwipeType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AsyncAction {

    @NotNull(message = "Target user ID is required")
    private Long targetUserId;

    @NotNull(message = "Swipe action is required")
    private SwipeType action;

}
