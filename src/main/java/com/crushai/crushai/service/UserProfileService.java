package com.crushai.crushai.service;

import com.crushai.crushai.dto.UserInfoDto;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.entity.UserInfoEntity;
import com.crushai.crushai.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserProfileService {
    private final UserRepository userRepository;

    public UserProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
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
}
