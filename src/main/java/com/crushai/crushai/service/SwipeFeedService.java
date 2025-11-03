package com.crushai.crushai.service;

import com.crushai.crushai.dto.SwipeCardDto;
import com.crushai.crushai.dto.SwipeFeedResponse;
import com.crushai.crushai.entity.Match;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.entity.UserInfoEntity;
import com.crushai.crushai.entity.UserSwipe;
import com.crushai.crushai.enums.Gender;
import com.crushai.crushai.repository.MatchRepository;
import com.crushai.crushai.repository.UserLikeRepository;
import com.crushai.crushai.repository.UserRepository;
import com.crushai.crushai.repository.UserSwipeRepository;
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
    private final MatchRepository matchRepository;
    
    private static final int INITIAL_BATCH_SIZE = 15;
    private static final int REFILL_BATCH_SIZE = 10;
    
    /**
     * 초기 Swipe 피드 가져오기
     */
    @Transactional(readOnly = true)
    public SwipeFeedResponse getInitialFeed(Long userId) {
        log.info("Fetching initial feed for user: {}", userId);
        
        UserEntity currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<SwipeCardDto> cards = getRecommendedUsers(currentUser, INITIAL_BATCH_SIZE, Collections.emptyList());
        
        return SwipeFeedResponse.builder()
            .users(cards)
            .totalCount(cards.size())
            .hasMore(!cards.isEmpty())
            .build();
    }
    
    /**
     * 추가 Swipe 피드 가져오기
     */
    @Transactional(readOnly = true)
    public SwipeFeedResponse getMoreFeed(Long userId, List<Long> excludeUserIds) {
        log.info("Fetching more feed for user: {}, excluding: {} users", userId, excludeUserIds.size());
        
        UserEntity currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<SwipeCardDto> cards = getRecommendedUsers(currentUser, REFILL_BATCH_SIZE, excludeUserIds);
        
        return SwipeFeedResponse.builder()
            .users(cards)
            .totalCount(cards.size())
            .hasMore(!cards.isEmpty())
            .build();
    }
    
    /**
     * 추천 유저 찾기 (간단한 버전)
     */
    private List<SwipeCardDto> getRecommendedUsers(
        UserEntity currentUser, 
        int limit,
        List<Long> additionalExcludeIds
    ) {
        Long userId = currentUser.getId();
        
        // 1. 이미 스와이프한 유저들
        Set<Long> swipedUserIds = swipeRepository
            .findAllByFromUserId(userId)
            .stream()
            .map(UserSwipe::getToUserId)
            .collect(Collectors.toSet());
        
        // 2. 이미 매칭된 유저들
        Set<Long> matchedUserIds = matchRepository
            .findActiveMatchesByUserId(userId)
            .stream()
            .map(match -> match.getUser1Id().equals(userId) 
                ? match.getUser2Id() 
                : match.getUser1Id())
            .collect(Collectors.toSet());
        
        // 3. 제외할 유저 통합
        Set<Long> excludedIds = new HashSet<>();
        excludedIds.add(userId); // 본인
        excludedIds.addAll(swipedUserIds);
        excludedIds.addAll(matchedUserIds);
        excludedIds.addAll(additionalExcludeIds);
        
        // 4. 선호 성별
        UserInfoEntity currentUserInfo = currentUser.getUserInfo();
        List<Gender> preferredGenders = currentUserInfo != null && currentUserInfo.getShowMeGender() != null
            ? currentUserInfo.getShowMeGender()
            : Arrays.asList(Gender.values());
        
        // 5. 후보 유저 찾기
        List<UserEntity> candidates = userRepository.findAll().stream()
            .filter(user -> !excludedIds.contains(user.getId()))
            .filter(UserEntity::isOnboardingCompleted)
            .filter(user -> !user.isDelYn())
            .filter(user -> user.getUserInfo() != null)
            .filter(user -> preferredGenders.contains(user.getUserInfo().getGender()))
            .collect(Collectors.toList());
        
        // 6. 랜덤 섞기
        Collections.shuffle(candidates);
        
        // 7. 필요한 수만큼
        List<UserEntity> selectedUsers = candidates.stream()
            .limit(limit)
            .toList();
        
        // 8. DTO 변환
        List<SwipeCardDto> cards = selectedUsers.stream()
            .map(this::convertToSwipeCardDto)
            .collect(Collectors.toList());
        
        // 9. 좋아요 상태 추가
        enrichWithLikeStatus(userId, cards);
        
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
            .likedByThem(false)
            .build();
    }
    
    /**
     * 각 유저가 현재 유저를 좋아요 했는지 확인
     */
    // 필요 없는 로직인것 같음.
    private void enrichWithLikeStatus(Long currentUserId, List<SwipeCardDto> cards) {
        if (cards.isEmpty()) return;
        
        List<Long> targetUserIds = cards.stream()
            .map(SwipeCardDto::getUserId)
            .collect(Collectors.toList());
        
        // 한 번의 쿼리로 모든 좋아요 정보 가져오기
        Map<Long, Boolean> likeStatusMap = likeRepository
            .findAllByFromUserIdInAndToUserId(targetUserIds, currentUserId)
            .stream()
            .collect(Collectors.toMap(
                like -> like.getFromUserId(),
                like -> true
            ));
        
        // 각 카드에 좋아요 상태 설정
        cards.forEach(card -> {
            boolean isLikedByThem = likeStatusMap.getOrDefault(card.getUserId(), false);
            card.setLikedByThem(isLikedByThem);
        });
    }
}
