package com.crushai.crushai.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MoreFeedRequest {
    
    @NotNull(message = "Exclude user IDs list is required")
    private List<Long> excludeUserIds;
}
