package com.crushai.crushai.service;

import com.crushai.crushai.client.ChatServiceClient;
import com.crushai.crushai.dto.*;
import com.crushai.crushai.entity.*;
import com.crushai.crushai.enums.MatchType;
import com.crushai.crushai.enums.SwipeType;
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

/**
 * Liked You 서비스
 * 
 * 나를 좋아한 유저 목록 조회 (범블 스타일)
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LikedYouService {

    private final UserLikeRepository likeRepository;
    private final UserRepository userRepository;
    private final MatchRepository matchRepository;
    private final UserBlockRepository blockRepository;
    private final UserSwipeRepository swipeRepository;
    private final ChatServiceClient chatServiceClient;
    private final NotificationService notificationService;

    /**
     * 나를 좋아한 유저 목록 조회
     * 
     * 1. 나를 좋아한 유저 목록 조회
     * 2. 차단 관계 필터링
     * 3. 매칭 관계 필터링
     * 4. 삭제된 유저 필터링
     * 5. 프로필 정보 조회
     * 6. 조합하여 반환
     * 
     * @param myUserId 내 유저 ID
     * @return Liked You 목록
     */
    @Transactional(readOnly = true)
    public LikedYouResponse getLikedYou(Long myUserId) {
        log.info("Fetching liked you for user: {}", myUserId);

        // 1. 나를 좋아한 유저 목록 (최신순)
        List<UserLike> allLikes = likeRepository.findAllByToUserIdOrderByCreatedAtDesc(myUserId);
        
        if (allLikes.isEmpty()) {
            log.info("No likes found for user: {}", myUserId);
            return LikedYouResponse.builder()
                    .likes(List.of())
                    .totalCount(0)
                    .build();
        }

        // 2. 차단 관계 제외
        Set<Long> blockedUserIds = getBlockedUserIds(myUserId);
        
        // 3. 이미 매칭된 유저 제외
        Set<Long> matchedUserIds = getMatchedUserIds(myUserId);
        
        // 4. 필터링
        List<UserLike> filteredLikes = allLikes.stream()
                .filter(like -> !blockedUserIds.contains(like.getFromUserId()))
                .filter(like -> !matchedUserIds.contains(like.getFromUserId()))
                .toList();

        // 5. 전체 개수
        int totalCount = filteredLikes.size();

        if (filteredLikes.isEmpty()) {
            return LikedYouResponse.builder()
                    .likes(List.of())
                    .totalCount(totalCount)
                    .build();
        }

        // 6. 유저 ID 목록 추출
        List<Long> likedUserIds = filteredLikes.stream()
                .map(UserLike::getFromUserId)
                .collect(Collectors.toList());

        // 7. 프로필 정보 조회
        Map<Long, UserInfoEntity> userInfoMap = getUserInfoMap(likedUserIds);

        // 8. DTO 조합
        List<LikedYouDto> likedYouList = filteredLikes.stream()
                .map(like -> buildLikedYouDto(like, userInfoMap))
                .filter(Objects::nonNull)  // 프로필 정보 없는 유저 제외
                .collect(Collectors.toList());

        log.info("Returning {} liked you for user: {}", likedYouList.size(), myUserId);

        return LikedYouResponse.builder()
                .likes(likedYouList)
                .totalCount(totalCount)
                .build();
    }

    /**
     * 차단 관계 유저 ID 가져오기
     */
    private Set<Long> getBlockedUserIds(Long myUserId) {
        // 내가 차단한 유저
        List<Long> blockedByMe = blockRepository.findBlockedUserIdsByBlockerId(myUserId);
        
        // 나를 차단한 유저
        List<Long> blockedMe = blockRepository.findBlockerIdsByBlockedUserId(myUserId);
        
        Set<Long> blockedUserIds = new HashSet<>(blockedByMe);
        blockedUserIds.addAll(blockedMe);
        
        return blockedUserIds;
    }

    /**
     * 매칭된 유저 ID 가져오기
     */
    private Set<Long> getMatchedUserIds(Long myUserId) {
        return matchRepository.findActiveMatchesByUserId(myUserId)
                .stream()
                .map(match -> match.getUser1Id().equals(myUserId) 
                        ? match.getUser2Id() 
                        : match.getUser1Id())
                .collect(Collectors.toSet());
    }

    /**
     * 프로필 정보 맵 가져오기
     */
    private Map<Long, UserInfoEntity> getUserInfoMap(List<Long> userIds) {
        return userRepository.findAllById(userIds)
                .stream()
                .filter(user -> !user.isDelYn())  // 삭제된 유저 제외
                .filter(user -> user.getUserInfo() != null)  // 온보딩 완료 유저만
                .collect(Collectors.toMap(
                        UserEntity::getId,
                        UserEntity::getUserInfo
                ));
    }

    /**
     * LikedYouDto 생성
     */
    private LikedYouDto buildLikedYouDto(
            UserLike like,
            Map<Long, UserInfoEntity> userInfoMap
    ) {
        Long fromUserId = like.getFromUserId();
        UserInfoEntity userInfo = userInfoMap.get(fromUserId);

        // 프로필 정보 없으면 null 반환
        if (userInfo == null) {
            log.warn("User info not found for user: {}", fromUserId);
            return null;
        }

        return LikedYouDto.builder()
                .likeId(like.getId())
                .likedAt(like.getCreatedAt())
                .userId(fromUserId)
                .name(userInfo.getNickname())
                .age(calculateAge(userInfo.getBirthDate()))
                .profilePhoto(userInfo.getPhotoUrls() != null && !userInfo.getPhotoUrls().isEmpty() 
                        ? userInfo.getPhotoUrls().get(0) 
                        : null)
                .locationName(userInfo.getLocationName())
                .build();
    }

    /**
     * 나이 계산
     */
    private Integer calculateAge(Date birthDate) {
        if (birthDate == null) {
            return null;
        }
        
        LocalDate birth = birthDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();
        LocalDate now = LocalDate.now();
        
        return Period.between(birth, now).getYears();
    }

    /**
     * Liked You 페이지에서 액션 수행 (LIKE 또는 PASS)
     * 
     * 로직:
     * 1. 나를 좋아한 유저인지 검증
     * 2. 기존 스와이프 기록 삭제 (있으면)
     * 3. 새로운 액션 기록 생성
     * 4. LIKE인 경우 매칭 확인 및 생성
     * 
     * @param myUserId 내 유저 ID
     * @param targetUserId 대상 유저 ID
     * @param action LIKE 또는 PASS
     * @return 액션 결과
     */
    @Transactional
    public LikedYouActionResponse processLikedYouAction(Long myUserId, Long targetUserId, SwipeType action) {
        log.info("Processing Liked You action: {} -> {} ({})", myUserId, targetUserId, action);

        // 1. 유저 검증
        UserEntity myUser = userRepository.findById(myUserId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        UserEntity targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Target user not found"));

        // 2. 나를 좋아한 유저인지 검증
        boolean isLikedByTarget = likeRepository.existsByFromUserIdAndToUserId(targetUserId, myUserId);
        if (!isLikedByTarget) {
            throw new IllegalStateException("Target user hasn't liked you");
        }

        // 3. 이미 매칭된 유저인지 확인
        boolean alreadyMatched = matchRepository.existsByUserIds(myUserId, targetUserId);
        if (alreadyMatched) {
            throw new IllegalStateException("Already matched with this user");
        }

        // 4. 기존 스와이프 기록 삭제 (PASS 했던 기록이 있을 수 있음)
        List<UserSwipe> existingSwipes = swipeRepository.findAllByFromUserIdAndToUserId(myUserId, targetUserId);
        if (!existingSwipes.isEmpty()) {
            swipeRepository.deleteAll(existingSwipes);
            log.info("Deleted {} existing swipe records for user {} -> {}", 
                    existingSwipes.size(), myUserId, targetUserId);
        }

        // 5. 새로운 액션 기록 생성
        UserSwipe newSwipe = UserSwipe.builder()
                .fromUserId(myUserId)
                .toUserId(targetUserId)
                .swipeType(action)
                .build();
        swipeRepository.save(newSwipe);

        // 6. PASS인 경우 바로 리턴
        if (action == SwipeType.PASS) {
            log.info("User {} passed on user {} from Liked You page", myUserId, targetUserId);
            return LikedYouActionResponse.passed();
        }

        // 7. LIKE인 경우 - 매칭 처리
        return handleLikeFromLikedYou(myUser, targetUser);
    }

    /**
     * Liked You에서 LIKE 했을 때 매칭 처리
     * 
     * 상대방이 나를 이미 좋아했으므로 무조건 매칭됨
     */
    private LikedYouActionResponse handleLikeFromLikedYou(UserEntity myUser, UserEntity targetUser) {
        Long myUserId = myUser.getId();
        Long targetUserId = targetUser.getId();

        log.info("Creating match: {} <-> {} (from Liked You)", myUserId, targetUserId);

        // 1. UserLike 기록 생성 (내가 상대방을 좋아함)
        UserLike myLike = UserLike.builder()
                .fromUserId(myUserId)
                .toUserId(targetUserId)
                .build();
        likeRepository.save(myLike);

        // 채팅방 UUID id 생성
        String chatRoomId = java.util.UUID.randomUUID().toString();

        // 2. 매칭 생성 (범블 방식: 여자가 먼저 좋아한 경우)
        Match match = Match.builder()
                .user1Id(myUserId)
                .user2Id(targetUserId)
                .matchType(MatchType.SWIPE)
                .chatRoomId(chatRoomId)
                .isActive(true)
                .build();
        Match savedMatch = matchRepository.save(match);

        log.info("Match created: matchId={}", savedMatch.getId());

        try {
            String createdChatRoomId = chatServiceClient.createChatRoomWithId(
                    savedMatch.getChatRoomId(),
                    savedMatch.getUser1Id(),
                    savedMatch.getUser2Id(),
                    savedMatch.getId()
            );
            log.info("Chat room created: chatRoomId={}", chatRoomId);

            // 검증: UUID가 일치하는지 확인
            if (!chatRoomId.equals(createdChatRoomId)) {
                log.warn("Chat room ID mismatch! Expected: {}, Actual: {}", chatRoomId, createdChatRoomId);
            }

        } catch (Exception e) {
            log.error("Failed to create chat room for match {}: {}", 
                    savedMatch.getId(), e.getMessage());
            // 채팅방 생성 실패해도 매칭은 유지
        }

        // 4. 푸시 알림 전송 (상대방에게)
        try {
            notificationService.sendMatchNotification(targetUser, myUser);
            log.info("Match notification sent to user {}", targetUserId);
        } catch (Exception e) {
            log.error("Failed to send match notification: {}", e.getMessage());
            // 푸시 실패해도 매칭은 유지
        }

        // 5. 응답 생성
        UserInfoEntity targetUserInfo = targetUser.getUserInfo();
        String profilePhoto = targetUserInfo != null &&
                              targetUserInfo.getPhotoUrls() != null &&
                              !targetUserInfo.getPhotoUrls().isEmpty()
                ? targetUserInfo.getPhotoUrls().get(0)
                : null;

        MatchedUserDto matchedUserDto = MatchedUserDto.builder()
                .userId(targetUserId)
                .nickname(targetUserInfo != null ? targetUserInfo.getNickname() : "Unknown")
                .profilePhoto(profilePhoto)
                .build();

        return LikedYouActionResponse.matched(chatRoomId, matchedUserDto);
    }

    /**
     * 나를 좋아한 유저 미리보기 조회 (프리 유저용)
     * 
     * 프로필 사진만 반환하여 클라이언트에서 블러 처리
     * 
     * @param myUserId 내 유저 ID
     * @return 프로필 사진 목록
     */
    @Transactional(readOnly = true)
    public LikedYouPreviewResponse getLikedYouPreview(Long myUserId) {
        log.info("Fetching liked you preview for user: {}", myUserId);

        // 1. 나를 좋아한 유저 목록 (최신순)
        List<UserLike> allLikes = likeRepository.findAllByToUserIdOrderByCreatedAtDesc(myUserId);
        
        if (allLikes.isEmpty()) {
            log.info("No likes found for user: {}", myUserId);
            return LikedYouPreviewResponse.builder()
                    .likes(List.of())
                    .totalCount(0)
                    .build();
        }

        // 2. 차단 관계 제외
        Set<Long> blockedUserIds = getBlockedUserIds(myUserId);
        
        // 3. 이미 매칭된 유저 제외
        Set<Long> matchedUserIds = getMatchedUserIds(myUserId);
        
        // 4. 필터링
        List<UserLike> filteredLikes = allLikes.stream()
                .filter(like -> !blockedUserIds.contains(like.getFromUserId()))
                .filter(like -> !matchedUserIds.contains(like.getFromUserId()))
                .toList();

        int totalCount = filteredLikes.size();

        if (filteredLikes.isEmpty()) {
            return LikedYouPreviewResponse.builder()
                    .likes(List.of())
                    .totalCount(totalCount)
                    .build();
        }

        // 5. 유저 ID 목록 추출
        List<Long> likedUserIds = filteredLikes.stream()
                .map(UserLike::getFromUserId)
                .collect(Collectors.toList());

        // 6. 프로필 정보 조회 (프로필 사진만 필요)
        Map<Long, UserInfoEntity> userInfoMap = getUserInfoMap(likedUserIds);

        // 7. 프로필 사진만 추출
        List<LikedYouPreviewDto> previewList = filteredLikes.stream()
                .map(like -> {
                    UserInfoEntity userInfo = userInfoMap.get(like.getFromUserId());
                    if (userInfo == null || userInfo.getPhotoUrls() == null || userInfo.getPhotoUrls().isEmpty()) {
                        return null;
                    }
                    return LikedYouPreviewDto.builder()
                            .profilePhoto(userInfo.getPhotoUrls().get(0))
                            .build();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("Returning {} preview photos for user: {}", previewList.size(), myUserId);

        return LikedYouPreviewResponse.builder()
                .likes(previewList)
                .totalCount(totalCount)
                .build();
    }
}
