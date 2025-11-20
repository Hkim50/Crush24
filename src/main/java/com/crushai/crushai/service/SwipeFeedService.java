package com.crushai.crushai.service;

import com.crushai.crushai.dto.NearbyUserDto;
import com.crushai.crushai.dto.SwipeCardDto;
import com.crushai.crushai.dto.SwipeFeedFilterRequest;
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
    private final UserLocationService userLocationService;
    
    private static final int INITIAL_BATCH_SIZE = 15;
    private static final int REFILL_BATCH_SIZE = 10;
    
    /**
     * 초기 Swipe 피드 가져오기 (필터 적용)
     */
    @Transactional(readOnly = true)
    public SwipeFeedResponse getInitialFeed(Long userId, SwipeFeedFilterRequest filter) {
        log.info("Fetching initial feed for user: {} with filter: age {}~{}, distance {}~{}km", 
                userId, filter.getMinAge(), filter.getMaxAge(), 
                filter.getMinDistanceKm(), filter.getMaxDistanceKm());
        
        UserEntity currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<SwipeCardDto> cards = getRecommendedUsers(currentUser, filter, INITIAL_BATCH_SIZE);
        
        return SwipeFeedResponse.builder()
            .users(cards)
            .totalCount(cards.size())
            .hasMore(!cards.isEmpty())
            .build();
    }
    
    /**
     * 추가 Swipe 피드 가져오기 (필터 적용)
     */
    @Transactional(readOnly = true)
    public SwipeFeedResponse getMoreFeed(Long userId, SwipeFeedFilterRequest filter) {
        log.info("Fetching more feed for user: {} with filter", userId);
        
        UserEntity currentUser = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        List<SwipeCardDto> cards = getRecommendedUsers(currentUser, filter, REFILL_BATCH_SIZE);
        
        return SwipeFeedResponse.builder()
            .users(cards)
            .totalCount(cards.size())
            .hasMore(!cards.isEmpty())
            .build();
    }
    
    /**
     * 추천 유저 찾기 (필터 적용 버전)
     * 1. 기본 제외 필터 (스와이프 완료, 차단 등)
     * 2. 나이 필터
     * 3. 거리 필터 (Redis Geo 사용)
     * 4. 선호 성별 필터
     */
    private List<SwipeCardDto> getRecommendedUsers(
            UserEntity currentUser, 
            SwipeFeedFilterRequest filter,
            int limit) {
        
        Long userId = currentUser.getId();
        
        // 1. 이미 스와이프한 유저들
        Set<Long> swipedUserIds = swipeRepository
            .findAllByFromUserId(userId)
            .stream()
            .map(UserSwipe::getToUserId)
            .collect(Collectors.toSet());
        
        // 2. 차단 관련 제외
        List<Long> blockedByMeIds = blockRepository.findBlockedUserIdsByBlockerId(userId);
        List<Long> blockedMeIds = blockRepository.findBlockerIdsByBlockedUserId(userId);
        
        // 3. 제외할 유저 통합
        Set<Long> excludedIds = new HashSet<>();
        excludedIds.add(userId);
        excludedIds.addAll(swipedUserIds);
        excludedIds.addAll(blockedByMeIds);
        excludedIds.addAll(blockedMeIds);
        
        log.info("Excluding {} users total", excludedIds.size());
        
        // 4. 선호 성별
        UserInfoEntity currentUserInfo = currentUser.getUserInfo();
        List<Gender> preferredGenders = currentUserInfo != null && currentUserInfo.getShowMeGender() != null
            ? currentUserInfo.getShowMeGender()
            : Arrays.asList(Gender.values());
        
        // 5. 거리 필터: Redis Geo로 반경 내 유저 조회
        List<NearbyUserDto> nearbyUsers = userLocationService.getUsersWithinRadius(
            userId, 
            filter.getMaxDistanceKm()
        );
        
        // 거리 필터링 (minDistance 이상)
        Map<Long, Double> nearbyUserDistances = nearbyUsers.stream()
            .filter(dto -> dto.distanceKm() >= filter.getMinDistanceKm())
            .collect(Collectors.toMap(
                NearbyUserDto::userId,
                NearbyUserDto::distanceKm
            ));
        
        log.info("Found {} users within distance range {}~{}km", 
                nearbyUserDistances.size(), filter.getMinDistanceKm(), filter.getMaxDistanceKm());
        
        // 6. DB에서 후보 유저 조회 (거리 필터 통과한 유저만)
        if (nearbyUserDistances.isEmpty()) {
            log.info("No users found in distance range");
            return Collections.emptyList();
        }
        
        List<UserEntity> candidates = userRepository.findAllById(nearbyUserDistances.keySet())
            .stream()
            .filter(user -> !excludedIds.contains(user.getId()))
            .filter(UserEntity::isOnboardingCompleted)
            .filter(user -> !user.isDelYn())
            .filter(user -> user.getUserInfo() != null)
            .filter(user -> preferredGenders.contains(user.getUserInfo().getGender()))
            .filter(user -> {
                // 7. 나이 필터
                int age = user.getUserInfo().getAge();
                return age >= filter.getMinAge() && age <= filter.getMaxAge();
            })
            .collect(Collectors.toList());
        
        log.info("After all filters: {} candidates", candidates.size());
        
        // 8. 랜덤 섞기
        Collections.shuffle(candidates);
        
        // 9. 필요한 수만큼
        List<UserEntity> selectedUsers = candidates.stream()
            .limit(limit)
            .toList();
        
        // 10. DTO 변환 (거리 정보 포함)
        return selectedUsers.stream()
            .map(user -> convertToSwipeCardDto(user, nearbyUserDistances.get(user.getId())))
            .collect(Collectors.toList());
    }
    
    /**
     * Entity를 DTO로 변환 (거리 정보 포함)
     */
    private SwipeCardDto convertToSwipeCardDto(UserEntity user, Double distanceKm) {
        UserInfoEntity userInfo = user.getUserInfo();
        
        return SwipeCardDto.builder()
            .userId(user.getId())
            .nickname(userInfo.getNickname())
            .age(userInfo.getAge())
            .distanceKm(distanceKm)
            .photos(userInfo.getPhotoUrls())
            .build();
    }
}
