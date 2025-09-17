package com.crushai.crushai.service;

import com.crushai.crushai.entity.LoginType;
import com.crushai.crushai.entity.Role;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.repository.RefreshRepository;
import com.crushai.crushai.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshRepository refreshRepository;

    @Test
    @DisplayName("존재하지 않는 유저를 탈퇴하면 예외가 발생한다")
    void deleteUser_whenUserNotFound_throwsException() {
        // given
        String nonExistentEmail = "nonexistent@test.com";
        given(userRepository.findByEmail(nonExistentEmail)).willReturn(Optional.empty());

        // when & then
        assertThrows(UsernameNotFoundException.class, () -> {
            userService.deleteUser(nonExistentEmail);
        });
    }

    @Test
    @DisplayName("정상 유저를 탈퇴하면 isDeleted가 true로 변경된다")
    void deleteUser_whenUserExists_marksAsDeleted() {
        // given
        String email = "test@test.com";
        UserEntity user = new UserEntity(email, Role.USER, LoginType.GOOGLE);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        userService.deleteUser(email);

        // then
        assertTrue(user.isDelYn());
        assertThat(user.getDeletedAt()).isNotNull();
        assertThat(user.getDeletedAt()).isAfter(Instant.now());
    }

    @Test
    @DisplayName("유저 탈퇴시 refreshToken이 삭제된다")
    void deleteUser_whenUserExists_deletesRefreshToken() {
        // given
        String email = "test@test.com";
        UserEntity user = new UserEntity(email, Role.USER, LoginType.GOOGLE);
        given(userRepository.findByEmail(email)).willReturn(Optional.of(user));

        // when
        userService.deleteUser(email);

        // then
        // refreshRepository.deleteAllByEmail(email)이 호출되었는지 검증
        verify(refreshRepository).deleteAllByEmail(email);
    }

}
