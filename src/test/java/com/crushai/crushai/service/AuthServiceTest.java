package com.crushai.crushai.service;

import com.crushai.crushai.auth.GoogleIdTokenValidator;
import com.crushai.crushai.entity.LoginType;
import com.crushai.crushai.entity.Role;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.jwt.JWTUtil;
import com.crushai.crushai.repository.RefreshRepository;
import com.crushai.crushai.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

import com.crushai.crushai.auth.AppleIdTokenValidator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.mockito.Mock;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RefreshRepository refreshRepository;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private GoogleIdTokenValidator googleValidator;

    // AuthService 생성자에 RestClient.Builder가 필요하므로 Mock 처리
    @Mock
    private RestClient.Builder restClientBuilder;
    @Mock
    private RestClient restClient;

    @Mock
    private AppleIdTokenValidator appleValidator;

    @Mock
    private ObjectMapper objectMapper;


    @BeforeEach
    void setUp() {
        // RestClient.Builder의 체이닝 동작을 모킹합니다.
        when(restClientBuilder.baseUrl(anyString())).thenReturn(restClientBuilder);
        when(restClientBuilder.build()).thenReturn(restClient);

        authService = new AuthService(userRepository, refreshRepository, jwtUtil, googleValidator, appleValidator, restClientBuilder, objectMapper);
    }

    private GoogleIdToken.Payload mockGooglePayload(String email, String googleId) {
        GoogleIdToken.Payload payload = mock(GoogleIdToken.Payload.class);
        when(payload.getEmail()).thenReturn(email);
        when(payload.getSubject()).thenReturn(googleId);
        return payload;
    }

    @Test
    @DisplayName("탈퇴한 유저가 복구 기간 이후 로그인하면 실패한다")
    void loginWithGoogle_whenUserDeletedAndAfterGracePeriod_throwsException() {
        // given
        String idToken = "test-google-id-token";
        String email = "deleted_user@test.com";
        String googleId = "google123";

        GoogleIdToken.Payload payload = mockGooglePayload(email, googleId);
        given(googleValidator.verify(idToken)).willReturn(payload);

        UserEntity deletedUser = new UserEntity(email, Role.USER, LoginType.GOOGLE, googleId, null);
        // 30일이 지난 시점으로 deletedAt 설정
        Instant deletedAt = Instant.now().minus(31, ChronoUnit.DAYS);
        deletedUser.deleteUser(deletedAt); // delYn=true, deletedAt=과거

        given(userRepository.findByGoogleId(googleId)).willReturn(Optional.of(deletedUser));

        // when & then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.loginWithGoogle(idToken);
        });

        assertEquals("탈퇴 후 30일이 지나 로그인할 수 없습니다.", exception.getMessage());
    }

    @Test
    @DisplayName("탈퇴한 유저가 복구 기간 내에 로그인하면 성공한다")
    void loginWithGoogle_whenUserDeletedAndWithinGracePeriod_succeeds() {
        // given
        String idToken = "test-google-id-token";
        String email = "deleted_user@test.com";
        String googleId = "google123";

        GoogleIdToken.Payload payload = mockGooglePayload(email, googleId);
        given(googleValidator.verify(idToken)).willReturn(payload);

        UserEntity deletedUser = new UserEntity(email, Role.USER, LoginType.GOOGLE, googleId, null);
        // 30일이 지나지 않은 시점으로 deletedAt 설정 (예: 10일 뒤 만료)
        Instant deletedAt = Instant.now().plus(10, ChronoUnit.DAYS);
        deletedUser.deleteUser(deletedAt); // delYn=true, deletedAt=미래

        assertTrue(deletedUser.isDelYn()); // 초기 상태는 탈퇴 상태

        given(userRepository.findByGoogleId(googleId)).willReturn(Optional.of(deletedUser));
        given(jwtUtil.createJwt(anyString(), anyString(), anyString(), any(Long.class)))
                .willReturn("fake-access-token", "fake-refresh-token");

        // when
        Map<String, String> tokens = authService.loginWithGoogle(idToken);

        // then
        // 유저가 복구되었는지 확인
        assertFalse(deletedUser.isDelYn());
        assertNull(deletedUser.getDeletedAt());

        // 토큰이 정상적으로 발급되었는지 확인
        assertThat(tokens).isNotNull();
        assertThat(tokens.get("accessToken")).isEqualTo("fake-access-token");
        assertThat(tokens.get("refreshToken")).isEqualTo("fake-refresh-token");
        // isReactivated 플래그가 true로 반환되는지 확인
        assertThat(tokens.get("isReactivated")).isEqualTo("true");

        // refresh token이 저장되었는지 확인
        verify(refreshRepository).save(any());
    }
}
