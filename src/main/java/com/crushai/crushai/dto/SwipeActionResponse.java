package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwipeActionResponse {
    private boolean isMatch;
    private String chatRoomId;
    private String message;
    private MatchedUserDto matchedUser;
}
