package com.crushai.crushai.controller;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.LikedYouActionRequest;
import com.crushai.crushai.dto.LikedYouActionResponse;
import com.crushai.crushai.dto.LikedYouPreviewResponse;
import com.crushai.crushai.dto.LikedYouResponse;
import com.crushai.crushai.service.LikedYouService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * Likes 관련 API
 * 
 * 범블 스타일: 나를 좋아한 유저 목록
 */
@RestController
@RequestMapping("/api/likes")
@RequiredArgsConstructor
@Slf4j
public class LikesController {

    private final LikedYouService likedYouService;

    /**
     * 나를 좋아한 유저 미리보기 조회 (프리 유저용)
     * 
     * GET /api/likes/received/preview
     * Authorization: Bearer {JWT}
     * 
     * Response:
     * {
     *   "likes": [
     *     {
     *       "profilePhoto": "https://..."
     *     }
     *   ],
     *   "totalCount": 15,
     * }
     * 
     * @param userDetails 인증된 유저 정보
     * @return 프로필 사진 목록 (블러 처리용)
     */
    @GetMapping("/received/preview")
    public ResponseEntity<LikedYouPreviewResponse> getLikedYouPreview(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getUserId();
        log.info("GET /api/likes/received/preview - userId: {}", userId);
        
        LikedYouPreviewResponse response = likedYouService.getLikedYouPreview(userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * 나를 좋아한 유저 목록 조회
     * 
     * GET /api/likes/received
     * Authorization: Bearer {JWT}
     * 
     * Response:
     * {
     *   "likes": [
     *     {
     *       "likeId": 123,
     *       "likedAt": "2024-11-23T15:30:00",
     *       "userId": 456,
     *       "name": "홍길동",
     *       "age": 25,
     *       "profilePhoto": "https://...",
     *       "locationName": "Seoul, KR"
     *     }
     *   ],
     *   "totalCount": 15
     * }
     * 
     * @param userDetails 인증된 유저 정보
     * @return 나를 좋아한 유저 목록
     */
    @GetMapping("/received")
    public ResponseEntity<LikedYouResponse> getLikedYou(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getUserId();
        log.info("GET /api/likes/received - userId: {}", userId);
        
        LikedYouResponse response = likedYouService.getLikedYou(userId);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Liked You 페이지에서 액션 수행 (LIKE 또는 PASS)
     * 
     * POST /api/likes/received/action
     * Authorization: Bearer {JWT}
     * 
     * Request Body:
     * {
     *   "targetUserId": 456,
     *   "action": "LIKE" | "PASS"
     * }
     * 
     * Response (LIKE - 매칭됨):
     * {
     *   "isMatch": true,
     *   "chatRoomId": "uuid-1234",
     *   "otherUser": {
     *     "userId": 456,
     *     "nickname": "홍길동",
     *     "profilePhoto": "https://..."
     *   },
     *   "message": "It's a match!"
     * }
     * 
     * Response (PASS):
     * {
     *   "isMatch": false,
     *   "message": "User passed"
     * }
     * 
     * @param userDetails 인증된 유저 정보
     * @param request 액션 요청
     * @return 액션 결과
     */
    @PostMapping("/received/action")
    public ResponseEntity<LikedYouActionResponse> performLikedYouAction(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody @Valid LikedYouActionRequest request) {
        
        Long userId = userDetails.getUserId();
        log.info("POST /api/likes/received/action - userId: {}, targetUserId: {}, action: {}", 
                userId, request.getTargetUserId(), request.getAction());
        
        LikedYouActionResponse response = likedYouService.processLikedYouAction(
                userId,
                request.getTargetUserId(),
                request.getAction()
        );
        
        return ResponseEntity.ok(response);
    }
}
