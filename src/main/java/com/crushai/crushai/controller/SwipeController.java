package com.crushai.crushai.controller;

import com.crushai.crushai.dto.*;
import com.crushai.crushai.service.SwipeActionService;
import com.crushai.crushai.service.SwipeFeedService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;

@RestController
@RequestMapping("/api/swipe")
@RequiredArgsConstructor
@Slf4j
public class SwipeController {

    private final SwipeFeedService swipeFeedService;
    private final SwipeActionService swipeActionService;

    /**
     * 초기 Swipe 피드 가져오기
     * GET /api/swipe/feed
     */
    @GetMapping("/feed")
    public ResponseEntity<SwipeFeedResponse> getInitialFeed(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        log.info("User {} requesting initial swipe feed", userId);
        
        SwipeFeedResponse response = swipeFeedService.getInitialFeed(userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 추가 Swipe 피드 가져오기
     * POST /api/swipe/feed/more
     */
    @PostMapping("/feed/more")
    public ResponseEntity<SwipeFeedResponse> getMoreFeed(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody @Valid MoreFeedRequest request
    ) {
        Long userId = userDetails.getUserId();
        log.info("User {} requesting more swipe feed, excluding {} users", 
                 userId, request.getExcludeUserIds().size());
        
        SwipeFeedResponse response = swipeFeedService.getMoreFeed(
            userId, 
            request.getExcludeUserIds() != null ? request.getExcludeUserIds() : Collections.emptyList()
        );
        return ResponseEntity.ok(response);
    }

    /**
     * Swipe 액션 수행 (LIKE/PASS)
     * POST /api/swipe/action
     */
    @PostMapping("/action")
    public ResponseEntity<SwipeActionResponse> performSwipe(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody @Valid SwipeActionRequest request
    ) {
        Long userId = userDetails.getUserId();
        log.info("User {} swiping {} on user {}", 
                 userId, request.getAction(), request.getTargetUserId());
        
        SwipeActionResponse response = swipeActionService.processSwipe(
            userId,
            request.getTargetUserId(),
            request.getAction()
        );
        
        return ResponseEntity.ok(response);
    }
}
