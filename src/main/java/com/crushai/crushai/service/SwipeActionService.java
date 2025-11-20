package com.crushai.crushai.service;

import com.crushai.crushai.client.ChatServiceClient;
import com.crushai.crushai.dto.AsyncAction;
import com.crushai.crushai.dto.MatchedUserDto;
import com.crushai.crushai.dto.SwipeActionResponse;
import com.crushai.crushai.entity.*;
import com.crushai.crushai.enums.MatchType;
import com.crushai.crushai.enums.SwipeType;
import com.crushai.crushai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SwipeActionService {

    private final UserRepository userRepository;
    private final UserSwipeRepository swipeRepository;
    private final UserLikeRepository likeRepository;
    private final MatchRepository matchRepository;
    private final ChatServiceClient chatServiceClient;
    private final NotificationService notificationService;

    /**
     * Swipe 액션 처리
     */
    @Transactional
    public SwipeActionResponse processSwipe(Long fromUserId, Long toUserId, SwipeType action) {
        log.info("Processing swipe: {} -> {} ({})", fromUserId, toUserId, action);
        
        // 1. 유저 검증
        UserEntity fromUser = userRepository.findById(fromUserId)
            .orElseThrow(() -> new IllegalArgumentException("From user not found"));
        UserEntity toUser = userRepository.findById(toUserId)
            .orElseThrow(() -> new IllegalArgumentException("To user not found"));
        
        // 2. 중복 스와이프 체크
        if (swipeRepository.existsByFromUserIdAndToUserId(fromUserId, toUserId)) {
            throw new IllegalStateException("Already swiped on this user");
        }
        
        // 3. Swipe 기록 저장
        UserSwipe swipe = UserSwipe.builder()
            .fromUserId(fromUserId)
            .toUserId(toUserId)
            .swipeType(action)
            .build();
        
        swipeRepository.save(swipe);
        
        // 4. LIKE인 경우 추가 처리
        if (action == SwipeType.LIKE) {
            return handleLike(fromUser, toUser);
        }
        
        // 5. PASS인 경우
        return SwipeActionResponse.builder()
            .isMatch(false)
            .message("Passed")
            .build();
    }

    /**
     * @Async("swipeExecutor")를 통해 별도 스레드 풀에서 실행
     * DB 쿼리를 최소화하여 성능 향상
     */
    @Async("swipeExecutor")
    @Transactional
    public void processAsyncSwipeBatch(List<AsyncAction> actions, Long userId) {
        log.info("[ASYNC] Processing {} swipes for user {} in thread: {}", 
                actions.size(), userId, Thread.currentThread().getName());
        
        try {
            // 1. 사용자 정보 한 번만 조회 (10번 → 1번)
            UserEntity fromUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
            
            // 2. 대상 사용자들 한 번에 조회 (10번 → 1번)
            List<Long> targetIds = actions.stream()
                .map(AsyncAction::getTargetUserId)
                .distinct()
                .toList();
            
            List<UserEntity> targetUsers = userRepository.findAllById(targetIds);
            
            // 빠른 조회를 위한 Map 생성
            Map<Long, UserEntity> targetUserMap = targetUsers.stream()
                .collect(java.util.stream.Collectors.toMap(UserEntity::getId, user -> user));
            
            // 3. 이미 스와이프한 사용자 필터링 (10번 → 1번)
            List<Long> existingSwipes = swipeRepository
                .findByFromUserIdAndToUserIdIn(userId, targetIds)
                .stream()
                .map(UserSwipe::getToUserId)
                .toList();
            
            int processedCount = 0;
            int skippedCount = 0;
            int matchCount = 0;
            List<Long> failedTargets = new java.util.ArrayList<>();
            
            // 4. 각 액션 처리
            for (AsyncAction action : actions) {
                try {
                    Long targetId = action.getTargetUserId();
                    
                    // 중복 체크
                    if (existingSwipes.contains(targetId)) {
                        log.debug("[ASYNC] Duplicate swipe ignored: {} -> {}", userId, targetId);
                        skippedCount++;
                        continue;
                    }
                    
                    // 대상 사용자 찾기
                    UserEntity toUser = targetUserMap.get(targetId);
                    if (toUser == null) {
                        log.warn("[ASYNC] Target user not found: {}", targetId);
                        failedTargets.add(targetId);
                        continue;
                    }
                    
                    // Swipe 저장
                    UserSwipe swipe = UserSwipe.builder()
                        .fromUserId(userId)
                        .toUserId(targetId)
                        .swipeType(action.getAction())
                        .build();
                    swipeRepository.save(swipe);
                    
                    // LIKE 처리
                    if (action.getAction() == SwipeType.LIKE) {
                        SwipeActionResponse response = handleLike(fromUser, toUser);
                        if (response.isMatch()) {
                            matchCount++;
                        }
                    }
                    
                    processedCount++;
                    
                } catch (Exception e) {
                    log.error("[ASYNC] Failed to process single swipe: {} -> {}", 
                        userId, action.getTargetUserId(), e);
                    failedTargets.add(action.getTargetUserId());
                }
            }
            
            // 5. 처리 결과 로깅
            log.info("[ASYNC] Batch completed for user {}: processed={}, skipped={}, matches={}, failed={}", 
                userId, processedCount, skippedCount, matchCount, failedTargets.size());
            
            if (!failedTargets.isEmpty()) {
                log.warn("[ASYNC] Failed target IDs for user {}: {}", userId, failedTargets);
            }
            
        } catch (Exception e) {
            log.error("[ASYNC] Batch swipe processing failed for user {}", userId, e);
            // TODO: 실패한 배치를 재시도 큐에 추가하거나 알림 발송
        }
    }

    /**
     * 좋아요 처리
     */
    private SwipeActionResponse handleLike(UserEntity fromUser, UserEntity toUser) {
        Long fromUserId = fromUser.getId();
        Long toUserId = toUser.getId();
        
        // 1. Like 저장
        UserLike like = UserLike.builder()
            .fromUserId(fromUserId)
            .toUserId(toUserId)
            .build();
        
        likeRepository.save(like);
        log.info("Like saved: {} -> {}", fromUserId, toUserId);
        
        // 2. 상호 좋아요 확인
        boolean isMutualLike = likeRepository.existsByFromUserIdAndToUserId(toUserId, fromUserId);
        
        if (!isMutualLike) {
            // 매칭 안됨 - 알림만 전송 (상대방이 나를 좋아요 했다는 알림)
            notificationService.sendLikeNotification(toUser, fromUser);
            
            return SwipeActionResponse.builder()
                .isMatch(false)
                .message("Like sent")
                .build();
        }
        
        // 3. 매칭 성공!
        log.info("MATCH! {} <-> {}", fromUserId, toUserId);
        return createMatch(fromUser, toUser);
    }
    /**
     * 매칭 생성
     */
    private SwipeActionResponse createMatch(UserEntity currentUser, UserEntity matchedUser) {
        Long currentUserId = currentUser.getId();
        Long matchedUserId = matchedUser.getId();
        
        // 1. 안전한 UUID 생성 (보안 강화)
        String chatRoomId = java.util.UUID.randomUUID().toString();
        
        // 2. Match 엔티티 생성 (UUID 포함)
        Match match = Match.builder()
            .user1Id(Math.min(currentUserId, matchedUserId))
            .user2Id(Math.max(currentUserId, matchedUserId))
            .matchType(MatchType.SWIPE)
            .chatRoomId(chatRoomId)  // UUID 미리 저장
            .isActive(true)
            .build();
        
        Match savedMatch = matchRepository.save(match);
        log.info("Match created with ID: {}, chatRoomId: {}", savedMatch.getId(), chatRoomId);
        
        // 3. 매칭 알림 발송 (양쪽 모두)
        notificationService.sendMatchNotification(currentUser, matchedUser);
        notificationService.sendMatchNotification(matchedUser, currentUser);
        
        // 4. 채팅방 생성 (UUID 전달)
        try {
            String createdChatRoomId = chatServiceClient.createChatRoomWithId(
                chatRoomId,  // 지정한 UUID 전달
                currentUserId,
                matchedUserId,
                savedMatch.getId()
            );
            
            log.info("Chat room created with UUID: {}", createdChatRoomId);
            
            // 검증: UUID가 일치하는지 확인
            if (!chatRoomId.equals(createdChatRoomId)) {
                log.warn("Chat room ID mismatch! Expected: {}, Actual: {}", chatRoomId, createdChatRoomId);
            }
            
        } catch (Exception e) {
            log.error("Failed to create chat room for match: {}", savedMatch.getId(), e);
            // 채팅방 생성 실패해도 매칭은 유지
            // UUID가 이미 Match에 저장되어 있으므로 나중에 재시도 가능
            // TODO: 재시도 로직 추가 (배치 작업 등)
        }
        
        // 5. 응답 생성 - 매칭된 상대방의 정보 반환
        UserInfoEntity matchedUserInfo = matchedUser.getUserInfo();
        String profilePhoto = matchedUserInfo != null && 
                              matchedUserInfo.getPhotoUrls() != null && 
                              !matchedUserInfo.getPhotoUrls().isEmpty()
            ? matchedUserInfo.getPhotoUrls().get(0)
            : null;
        
        return SwipeActionResponse.builder()
            .isMatch(true)
            .chatRoomId(chatRoomId)  // UUID 반환
            .message("It's a match!")
            .matchedUser(MatchedUserDto.builder()
                .userId(matchedUserId)
                .nickname(matchedUserInfo != null ? matchedUserInfo.getNickname() : "Unknown")
                .profilePhoto(profilePhoto)
                .build())
            .build();
    }
}
