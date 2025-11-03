package com.crushai.crushai.service;

import com.crushai.crushai.dto.UserInfoDto;
import com.crushai.crushai.entity.Match;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.entity.UserInfoEntity;
import com.crushai.crushai.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class UserService {
    private final UserRepository userRepository;
    private final RefreshRepository refreshRepository;
    private final MatchRepository matchRepository;
    private final UserSwipeRepository userSwipeRepository;
    private final UserLikeRepository userLikeRepository;

    public UserService(UserRepository userRepository, 
                      RefreshRepository refreshRepository,
                      MatchRepository matchRepository,
                      UserSwipeRepository userSwipeRepository,
                      UserLikeRepository userLikeRepository) {
        this.userRepository = userRepository;
        this.refreshRepository = refreshRepository;
        this.matchRepository = matchRepository;
        this.userSwipeRepository = userSwipeRepository;
        this.userLikeRepository = userLikeRepository;
    }

    public UserInfoDto getUser(String email) {
        // 1. Find the user by email. If not found, throw an exception.
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 2. Get the associated UserInfo. If the user hasn't completed onboarding, this will be null.
        UserInfoEntity userInfo = user.getUserInfo();

        // 3. If UserInfo is null, it means the profile doesn't exist yet. Return null.
        if (userInfo == null) {
            return null;
        }

        // 4. If UserInfo exists, convert it to a DTO and return it.
        return userInfo.toDto();
    }


    @Transactional
    public UserInfoDto updateProfile(String email, UserInfoDto userInfoDto) {
        // 1. 사용자 엔티티를 찾습니다.
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 2. 연결된 UserInfo 엔티티를 가져옵니다. 온보딩을 안했다면 업데이트할 수 없습니다.
        UserInfoEntity userInfo = user.getUserInfo();
        if (userInfo == null) {
            throw new IllegalStateException("Cannot update profile for a user who has not completed onboarding.");
        }

        userInfo.updateProfile(userInfoDto);

        // 4. 변경된 엔티티를 다시 DTO로 변환하여 반환합니다.
        return userInfo.toDto();
    }

    @Transactional
    public void deleteUser(String email) {
        // 1. 이메일로 유저를 찾습니다. 없으면 예외를 발생시킵니다.
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));

        // 2. 해당 유저의 모든 리프레시 토큰을 삭제합니다.
        refreshRepository.deleteAllByEmail(email);

        // 3. 유저의 delYn 플래그를 true로 변경합니다.
        user.deleteUser(Instant.now().plus(30, ChronoUnit.DAYS));
        
        log.info("User {} marked for deletion. Will be deleted after 30 days.", user.getId());
    }

    @Transactional
    public void deleteExpiredUsers(Instant now) {
        // delYn이 true이고 deletedAt이 현재 시간 이전인 유저들을 조회
        List<UserEntity> usersToDelete = userRepository.findAllByDelYnTrueAndDeletedAtBefore(now);
        
        if (usersToDelete.isEmpty()) {
            log.info("삭제할 만료된 사용자가 없습니다.");
            return;
        }
        
        log.info("만료된 사용자 {}명을 삭제 시작합니다.", usersToDelete.size());
        
        // 삭제할 유저 ID 목록
        List<Long> userIdsToDelete = usersToDelete.stream()
            .map(UserEntity::getId)
            .collect(Collectors.toList());
        
        // 1. Matches 정리 - 양쪽 유저가 모두 삭제될 매칭만 삭제
        cleanupMatches(userIdsToDelete);
        
        // 2. UserSwipes 삭제 (CASCADE로 처리되지 않으므로 수동 삭제)
        cleanupUserSwipes(userIdsToDelete);
        
        // 3. UserLikes 삭제 (CASCADE로 처리되지 않으므로 수동 삭제)
        cleanupUserLikes(userIdsToDelete);
        
        // 4. User 삭제 (UserInfo는 CASCADE로 자동 삭제)
        userRepository.deleteAll(usersToDelete);

        // TODO
        // 채팅 프로젝트에서 채팅 및 채팅방 삭제 필요
        
        log.info("만료된 사용자 {}명 삭제 완료!", usersToDelete.size());
    }
    
    /**
     * Matches 정리
     * - 이미 비활성화된 매칭: 삭제 (두 번째 유저도 탈퇴)
     * - 활성화된 매칭: 비활성화 (첫 번째 유저 탈퇴)
     */
    private void cleanupMatches(List<Long> userIdsToDelete) {
        // 삭제될 유저가 포함된 모든 매칭 조회
        List<Match> affectedMatches = matchRepository.findMatchesWithDeletedUsers(userIdsToDelete);

        if (affectedMatches.isEmpty()) {
            log.info("정리할 매칭이 없습니다.");
            return;
        }

        int deletedCount = 0;
        int deactivatedCount = 0;
        
        for (Match match : affectedMatches) {
            if (!match.isActive()) {
                // 이미 비활성화됨 → 두 번째 유저도 삭제 → 매칭 삭제
                matchRepository.delete(match);
                deletedCount++;
                log.debug("Match {} deleted (both users deleted)", match.getId());
            } else {
                // 활성화됨 → 첫 번째 유저 삭제 → 매칭 비활성화
                match.deactivate();
                deactivatedCount++;
                log.debug("Match {} deactivated (first user deleted)", match.getId());
            }
        }
        
        log.info("Matches 정리 완료 - 삭제: {}개, 비활성화: {}개", deletedCount, deactivatedCount);
    }
    
    /**
     * UserSwipes 정리
     * - 삭제될 유저가 from_user 또는 to_user인 모든 스와이프 삭제
     */
    private void cleanupUserSwipes(List<Long> userIdsToDelete) {
        for (Long userId : userIdsToDelete) {
            // from_user_id = userId인 스와이프 삭제
            int deletedFromCount = userSwipeRepository.deleteByFromUserId(userId);
            
            // to_user_id = userId인 스와이프 삭제
            int deletedToCount = userSwipeRepository.deleteByToUserId(userId);
            
            log.debug("User {} swipes deleted - from: {}, to: {}", userId, deletedFromCount, deletedToCount);
        }
        
        log.info("UserSwipes 정리 완료");
    }
    
    /**
     * UserLikes 정리
     * - 삭제될 유저가 from_user 또는 to_user인 모든 좋아요 삭제
     */
    private void cleanupUserLikes(List<Long> userIdsToDelete) {
        for (Long userId : userIdsToDelete) {
            // from_user_id = userId인 좋아요 삭제
            int deletedFromCount = userLikeRepository.deleteByFromUserId(userId);
            
            // to_user_id = userId인 좋아요 삭제
            int deletedToCount = userLikeRepository.deleteByToUserId(userId);
            
            log.debug("User {} likes deleted - from: {}, to: {}", userId, deletedFromCount, deletedToCount);
        }
        
        log.info("UserLikes 정리 완료");
    }
}
