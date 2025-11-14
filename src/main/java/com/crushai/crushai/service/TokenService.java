package com.crushai.crushai.service;

import com.crushai.crushai.entity.RefreshEntity;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.jwt.JWTUtil;
import com.crushai.crushai.repository.RefreshRepository;
import com.crushai.crushai.repository.UserRepository;
import io.jsonwebtoken.ExpiredJwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;
    private final UserRepository userRepository;

    /**
     * Refresh Token으로 Access Token 재발급
     * 
     * @param refreshToken 클라이언트로부터 받은 refresh token
     * @return 새로운 access token과 refresh token
     */
    @Transactional
    public ResponseEntity<?> reissueTokens(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "refresh token is missing"));
        }

        // 1. 토큰 만료 검사
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            log.warn("Expired refresh token attempted: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "refresh token expired"));
        }

        // 2. refresh 토큰 타입 확인
        String category = jwtUtil.getCategory(refreshToken);
        if (!"refreshToken".equals(category)) {
            log.warn("Invalid token category: {}", category);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "invalid token type"));
        }

        // 3. 토큰 해시 생성 및 DB 존재 확인
        String tokenHash = hashToken(refreshToken);
        RefreshEntity storedToken = refreshRepository.findByTokenHash(tokenHash)
            .orElse(null);

        if (storedToken == null) {
            log.warn("Refresh token not found in database");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "refresh token not found"));
        }

        // 4. 토큰 만료 확인
        if (storedToken.isExpired()) {
            log.warn("Stored token is expired for user: {}", storedToken.getUser().getId());
            refreshRepository.delete(storedToken);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("error", "refresh token expired"));
        }

        // 5. 사용자 정보 추출
        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);
        Long userId = jwtUtil.getUserId(refreshToken);

        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        // 6. 새로운 토큰 생성
        long accessExpirationMs = 3600_000L;  // 1시간
        long refreshExpirationMs = 14L * 24 * 3600_000L; // 14일
        
        String newAccessToken = jwtUtil.createJwt("accessToken", username, role, userId, accessExpirationMs);
        String newRefreshToken = jwtUtil.createJwt("refreshToken", username, role, userId, refreshExpirationMs);

        // 7. 기존 토큰 삭제 및 새 토큰 저장 (Rotation 방식)
        refreshRepository.delete(storedToken);
        
        String newTokenHash = hashToken(newRefreshToken);
        Instant expiresAt = Instant.now().plusMillis(refreshExpirationMs);
        
        RefreshEntity newRefreshEntity = RefreshEntity.builder()
            .user(storedToken.getUser())
            .tokenHash(newTokenHash)
            .deviceId(storedToken.getDeviceId())
            .deviceType(storedToken.getDeviceType())
            .deviceName(storedToken.getDeviceName())
            .expiresAt(expiresAt)
            .lastUsedAt(Instant.now())
            .build();
        
        refreshRepository.save(newRefreshEntity);

        log.info("Token reissued successfully for user: {}, device: {}", 
            userId, storedToken.getDeviceId());

        // 8. 응답 생성
        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccessToken);
        tokens.put("refreshToken", newRefreshToken);

        return ResponseEntity.ok(tokens);
    }

    /**
     * Refresh Token을 SHA-256으로 해시
     * 
     * @param token 원본 JWT 토큰
     * @return SHA-256 해시 (64자 hex 문자열)
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            // byte array를 hex string으로 변환
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
