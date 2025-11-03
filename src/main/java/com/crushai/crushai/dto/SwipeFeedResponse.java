package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwipeFeedResponse {
    private List<SwipeCardDto> users;
    private int totalCount;
    private boolean hasMore;
}
