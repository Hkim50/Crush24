package com.crushai.crushai.service;

import com.crushai.crushai.dto.UserInfoDto;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.entity.UserInfoEntity;
import com.crushai.crushai.repository.RefreshRepository;
import com.crushai.crushai.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final RefreshRepository refreshRepository;

    public UserService(UserRepository userRepository, RefreshRepository refreshRepository) {
        this.userRepository = userRepository;
        this.refreshRepository = refreshRepository;
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
    }

    @Transactional
    public void deleteExpiredUsers(Instant now) {
        List<UserEntity> usersToDelete = userRepository.findAllByDeletedTrueAndDeletedAtBefore(now);
        userRepository.deleteAll(usersToDelete);
    }


}
