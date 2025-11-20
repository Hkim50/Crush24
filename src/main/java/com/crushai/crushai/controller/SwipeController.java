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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/swipe")
@RequiredArgsConstructor
@Slf4j
public class SwipeController {

    private final SwipeFeedService swipeFeedService;
    private final SwipeActionService swipeActionService;

    /**
     * 초기 Swipe 피드 가져오기 (필터 적용)
     * POST /api/swipe/feed
     * 
     * Request Body:
     * {
     *   "minAge": 20,
     *   "maxAge": 25,
     *   "minDistanceKm": 1.0,
     *   "maxDistanceKm": 20.0
     * }
     */
    @PostMapping("/feed")
    public ResponseEntity<SwipeFeedResponse> getInitialFeed(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody @Valid SwipeFeedFilterRequest filter
    ) {
        Long userId = userDetails.getUserId();
        log.info("User {} requesting initial swipe feed with filter", userId);
        
        filter.validate();
        SwipeFeedResponse response = swipeFeedService.getInitialFeed(userId, filter);
        return ResponseEntity.ok(response);
    }

    /**
     * 추가 Swipe 피드 가져오기 (필터 적용)
     * POST /api/swipe/feed/more
     */
    @PostMapping("/feed/more")
    public ResponseEntity<SwipeFeedResponse> getMoreFeed(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody @Valid SwipeFeedFilterRequest filter
    ) {
        Long userId = userDetails.getUserId();
        log.info("User {} requesting more swipe feed with filter", userId);
        
        filter.validate();
        SwipeFeedResponse response = swipeFeedService.getMoreFeed(userId, filter);
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

    /**
     * 비동기 Swipe 액션 배치 처리
     * 앱에서 10개의 swipe 데이터가 쌓였을 때 한 번에 전송
     * 
     * POST /api/swipe/action/async
     */
    @PostMapping("/action/async")
    public ResponseEntity<Map<String, Object>> processAsyncSwipes(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid List<AsyncAction> actions
    ) {
        Long userId = userDetails.getUserId();
        
        // 요청 검증
        if (actions == null || actions.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Actions list cannot be empty"));
        }
        
        if (actions.size() > 20) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Cannot process more than 20 actions at once"));
        }
        
        log.info("User {} submitted {} async swipes", userId, actions.size());

        // @Async 메서드 호출 - Spring이 자동으로 별도 스레드에서 실행
        swipeActionService.processAsyncSwipeBatch(actions, userId);

        // 즉시 202 Accepted 응답
        return ResponseEntity.accepted()
            .body(Map.of(
                "message", "Swipes are being processed",
                "count", actions.size()
            ));
    }


}
