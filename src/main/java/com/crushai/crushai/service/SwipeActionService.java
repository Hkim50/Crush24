package com.crushai.crushai.service;

import com.crushai.crushai.client.ChatServiceClient;
import com.crushai.crushai.dto.MatchedUserDto;
import com.crushai.crushai.dto.SwipeActionResponse;
import com.crushai.crushai.entity.*;
import com.crushai.crushai.enums.MatchType;
import com.crushai.crushai.enums.SwipeType;
import com.crushai.crushai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        
        // 1. Match 엔티티 생성 (작은 ID를 user1으로)
        Match match = Match.builder()
            .user1Id(Math.min(currentUserId, matchedUserId))
            .user2Id(Math.max(currentUserId, matchedUserId))
            .matchType(MatchType.SWIPE)
            .isActive(true)
            .build();
        
        Match savedMatch = matchRepository.save(match);
        log.info("Match created with ID: {}", savedMatch.getId());
        
        // 2. 매칭 알림 발송 (양쪽 모두)
        notificationService.sendMatchNotification(currentUser, matchedUser);
        notificationService.sendMatchNotification(matchedUser, currentUser);
        
        // 3. 채팅방 생성 (비동기로 처리 - 실패해도 재시도 가능)
        String chatRoomId = null;
        try {
            chatRoomId = chatServiceClient.createChatRoom(
                currentUserId,
                matchedUserId,
                savedMatch.getId()
            );
            
            // 4. chatRoomId 업데이트
            savedMatch.setChatRoomId(chatRoomId);
            matchRepository.save(savedMatch);
            
            log.info("Chat room created: {}", chatRoomId);
        } catch (Exception e) {
            log.error("Failed to create chat room for match: {}", savedMatch.getId(), e);
            // 채팅방 생성 실패해도 매칭은 유지
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
            .chatRoomId(chatRoomId)
            .message("It's a match!")
            .matchedUser(MatchedUserDto.builder()
                .userId(matchedUserId)
                .nickname(matchedUserInfo != null ? matchedUserInfo.getNickname() : "Unknown")
                .profilePhoto(profilePhoto)
                .build())
            .build();
    }
}
