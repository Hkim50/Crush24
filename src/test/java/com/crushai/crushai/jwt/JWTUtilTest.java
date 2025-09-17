package com.crushai.crushai.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class JWTUtilTest {

    private JWTUtil jwtUtil;
    private final String secret = "this-is-a-very-long-and-secure-secret-key-for-testing-purpose-only"; // 32바이트 이상의 시크릿 키
    private SecretKey testSecretKey;

    @BeforeEach
    void setUp() {
        jwtUtil = new JWTUtil(secret);
        // 테스트 검증용 SecretKey를 테스트 클래스 내에서 직접 생성
        testSecretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Test
    @DisplayName("JWT 생성 시 발급 시간과 만료 시간이 올바르게 설정된다")
    void createJwt_setsIssuedAtAndExpirationCorrectly() {
        // given
        long expiredMs = 3600_000L; // 1시간
        Instant beforeCreation = Instant.now();

        // when
        String token = jwtUtil.createJwt("accessToken", "test@test.com", "USER", expiredMs);

        // then
        Instant afterCreation = Instant.now();
        Claims claims = Jwts.parser().verifyWith(testSecretKey).build().parseSignedClaims(token).getPayload();

        Date issuedAt = claims.getIssuedAt();
        Date expiration = claims.getExpiration();

        // 발급 시간(iat)이 토큰 생성 시점과 거의 일치하는지 확인
        assertThat(issuedAt.toInstant()).isBetween(beforeCreation.minusSeconds(1), afterCreation.plusSeconds(1));

        // 만료 시간(exp)이 발급 시간 + 만료 기간과 일치하는지 확인
        long expectedExpirationMillis = issuedAt.getTime() + expiredMs;
        assertThat(expiration.getTime()).isEqualTo(expectedExpirationMillis);
    }

    @Test
    @DisplayName("만료된 토큰은 isExpired가 true를 반환한다")
    void isExpired_returnsTrue_forExpiredToken() throws InterruptedException {
        // given
        // 만료 시간을 1밀리초로 설정하여 즉시 만료되도록 토큰 생성
        String expiredToken = jwtUtil.createJwt("accessToken", "test@test.com", "USER", 1L);

        // 토큰 생성과 검증 사이에 시간차를 두어 확실히 만료되도록 함
        Thread.sleep(5);

        // when & then
        // isExpired 메서드 내부의 getExpirationDate가 토큰 파싱 시 ExpiredJwtException을 던지는지 확인합니다.
        // 이 예외가 발생한다는 것 자체가 토큰이 만료되었음을 의미합니다.
        assertThrows(ExpiredJwtException.class, () -> {
            jwtUtil.isExpired(expiredToken);
        });
    }

    @Test
    @DisplayName("만료되지 않은 토큰은 isExpired가 false를 반환한다")
    void isExpired_returnsFalse_forValidToken() {
        // given
        // 만료 시간을 1시간으로 설정한 유효한 토큰 생성
        String validToken = jwtUtil.createJwt("accessToken", "test@test.com", "USER", 3600_000L);

        // when
        boolean isExpired = jwtUtil.isExpired(validToken);

        // then
        assertFalse(isExpired);
    }
}