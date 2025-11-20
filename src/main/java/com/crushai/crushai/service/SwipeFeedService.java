package com.crushai.crushai.service;

import com.crushai.crushai.dto.SwipeCardDto;
import com.crushai.crushai.dto.SwipeFeedResponse;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.entity.UserInfoEntity;
import com.crushai.crushai.entity.UserSwipe;
import com.crushai.crushai.enums.Gender;
import com.crushai.crushai.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SwipeFeedService {

    private final UserRepository userRepository;
    private final UserSwipeRepository swipeRepository;
    private final UserLikeRepository likeRepository;
    private final UserBlockRepository blockRepository;
    
    private static final int INITIAL_BATCH_SIZE = 15;
    private static final int REFILL_BATCH_SIZE = 10;
    
    /**
     * 초기 Swipe 피드 가져오기
     * excludeUserIds 파라미터 제거 - 서버에서 자동 필터링
     */
    @Transactional(readOnly = true)
    public SwipeFeedResponse getInitialFeed(Long userId) {
        log.info("Fetching initial feed for user: {}", userId);
        
        UserEntity currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<SwipeCardDto> cards = getRecommendedUsers(currentUser, INITIAL_BATCH_SIZE);
        
        return SwipeFeedResponse.builder()
            .users(cards)
            .totalCount(cards.size())
            .hasMore(!cards.isEmpty())
            .build();
    }
    
    /**
     * 추가 Swipe 피드 가져오기
     * excludeUserIds 파라미터 제거 - 서버에서 자동 필터링
     */
    @Transactional(readOnly = true)
    public SwipeFeedResponse getMoreFeed(Long userId) {
        log.info("Fetching more feed for user: {}", userId);
        
        UserEntity currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<SwipeCardDto> cards = getRecommendedUsers(currentUser, REFILL_BATCH_SIZE);
        
        return SwipeFeedResponse.builder()
            .users(cards)
            .totalCount(cards.size())
            .hasMore(!cards.isEmpty())
            .build();
    }
    
    /**
     * 추천 유저 찾기 (개선 버전)
     * 서버에서 모든 필터링 자동 수행
     */
    private List<SwipeCardDto> getRecommendedUsers(UserEntity currentUser, int limit) {
        Long userId = currentUser.getId();
        
        // 1. 이미 스와이프한 유저들 (LIKE든 PASS든 모두 포함)
        //    → 매칭된 유저도 여기 포함됨 (매칭 = 양방향 스와이프)
        Set<Long> swipedUserIds = swipeRepository
            .findAllByFromUserId(userId)
            .stream()
            .map(UserSwipe::getToUserId)
            .collect(Collectors.toSet());
        
        // 2. 내가 차단한 유저들
        List<Long> blockedByMeIds = blockRepository.findBlockedUserIdsByBlockerId(userId);
        
        // 3. 나를 차단한 유저들
        List<Long> blockedMeIds = blockRepository.findBlockerIdsByBlockedUserId(userId);
        
        // 4. 제외할 유저 통합
        Set<Long> excludedIds = new HashSet<>();
        excludedIds.add(userId); // 본인
        excludedIds.addAll(swipedUserIds); // 스와이프한 유저 (매칭된 유저 포함)
        excludedIds.addAll(blockedByMeIds); // 차단한 유저
        excludedIds.addAll(blockedMeIds); // 나를 차단한 유저
        
        log.info("Excluding {} users: {} swiped, {} blocked by me, {} blocked me",
                excludedIds.size(), swipedUserIds.size(), blockedByMeIds.size(), blockedMeIds.size());
        
        // 5. 선호 성별
        UserInfoEntity currentUserInfo = currentUser.getUserInfo();
        List<Gender> preferredGenders = currentUserInfo != null && currentUserInfo.getShowMeGender() != null
            ? currentUserInfo.getShowMeGender()
            : Arrays.asList(Gender.values());
        
        // 6. 후보 유저 찾기 (TODO: 나중에 쿼리 최적화 필요)
        List<UserEntity> candidates = userRepository.findAll().stream()
            .filter(user -> !excludedIds.contains(user.getId()))
            .filter(UserEntity::isOnboardingCompleted)
            .filter(user -> !user.isDelYn())
            .filter(user -> user.getUserInfo() != null)
            .filter(user -> preferredGenders.contains(user.getUserInfo().getGender()))
            .collect(Collectors.toList());
        
        // 7. 랜덤 섞기
        Collections.shuffle(candidates);
        
        // 8. 필요한 수만큼
        List<UserEntity> selectedUsers = candidates.stream()
            .limit(limit)
            .toList();
        
        // 9. DTO 변환
        List<SwipeCardDto> cards = selectedUsers.stream()
            .map(this::convertToSwipeCardDto)
            .collect(Collectors.toList());
        
        // 10. 좋아요 상태 추가
//        enrichWithLikeStatus(userId, cards);
        
        return cards;
    }
    
    /**
     * Entity를 DTO로 변환
     */
    private SwipeCardDto convertToSwipeCardDto(UserEntity user) {
        UserInfoEntity userInfo = user.getUserInfo();
        
        Integer age = null;
        if (userInfo.getBirthDate() != null) {
            LocalDate birthDate = userInfo.getBirthDate().toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
            age = Period.between(birthDate, LocalDate.now()).getYears();
        }
        
        return SwipeCardDto.builder()
            .userId(user.getId())
            .nickname(userInfo.getNickname())
            .age(age)
            .location(userInfo.getLocation())
            .photos(userInfo.getPhotoUrls())
            .build();
    }
    
    /**
     * 각 카드에 "상대방이 나를 좋아요 했는지" 상태 추가
     * 
     * 비즈니스 목적:
     * - UI에서 "Likes you!" 배지 표시
     * - 매칭 확률 증가 (상대가 이미 관심 있음)
     * - 향후 프리미엄 기능 수익화 가능
     * 
     * 성능: IN 쿼리 1개로 배치 처리 (효율적)
     */
//    private void enrichWithLikeStatus(Long currentUserId, List<SwipeCardDto> cards) {
//        if (cards.isEmpty()) return;
//
//        List<Long> targetUserIds = cards.stream()
//            .map(SwipeCardDto::getUserId)
//            .collect(Collectors.toList());
//
//        // 한 번의 쿼리로 모든 좋아요 정보 가져오기
//        Map<Long, Boolean> likeStatusMap = likeRepository
//            .findAllByFromUserIdInAndToUserId(targetUserIds, currentUserId)
//            .stream()
//            .collect(Collectors.toMap(
//                like -> like.getFromUserId(),
//                like -> true
//            ));
//
//        // 각 카드에 좋아요 상태 설정
//        cards.forEach(card -> {
//            boolean isLikedByThem = likeStatusMap.getOrDefault(card.getUserId(), false);
//            card.setLikedByThem(isLikedByThem);
//        });
//    }
}
