package com.crushai.crushai.jwt;

import com.crushai.crushai.entity.Role;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

@Component
public class JWTUtil {
    private final SecretKey secretKey;

    public JWTUtil(@Value("${spring.jwt.secret}")String secret) {
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("email", String.class);
    }

    public String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public String getCategory(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("category", String.class);
    }

    // 만료일 Date 객체를 직접 반환하는 메서드 추가
    public Date getExpirationDate(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token must not be null or empty");
        }
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().getExpiration();
    }

    public Boolean isExpired(String token) {
        // UTC 기준의 현재 시간과 비교하여 일관성을 유지합니다.
        return getExpirationDate(token).before(Date.from(Instant.now()));
    }

    public String createJwt(String category, String username, String role, Long expiredMs) {
        Instant now = Instant.now(); // UTC 기준 현재 시간
        return Jwts.builder()
                .claim("category", category)
                .claim("email", username)
                .claim("role", role)
                .issuedAt(Date.from(now)) // Instant를 Date로 변환하여 설정
                .expiration(Date.from(now.plusMillis(expiredMs))) // Instant를 Date로 변환하여 설정
                .signWith(secretKey)
                .compact();
    }
}
