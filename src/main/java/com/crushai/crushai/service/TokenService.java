package com.crushai.crushai.service;

import com.crushai.crushai.entity.RefreshEntity;
import com.crushai.crushai.jwt.JWTUtil;
import com.crushai.crushai.repository.RefreshRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class TokenService {

    private final JWTUtil jwtUtil;
    private final RefreshRepository repository;

    public TokenService(JWTUtil jwtUtil, RefreshRepository repository) {
        this.jwtUtil = jwtUtil;
        this.repository = repository;
    }

    public ResponseEntity<?> reissueTokens(String refreshToken) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("refresh token is missing");
        }

        // 토큰 만료 검사
        try {
            jwtUtil.isExpired(refreshToken);
        } catch (ExpiredJwtException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("refresh token expired");
        }

        // refresh인지 확인
        String category = jwtUtil.getCategory(refreshToken);
        if (!"refreshToken".equals(category)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("invalid token type");
        }

        Boolean isExist = repository.existsByRefresh(refreshToken);

        if (!isExist) {
            //response body
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("refresh token not found");
        }

        String username = jwtUtil.getUsername(refreshToken);
        String role = jwtUtil.getRole(refreshToken);
        Long userId = jwtUtil.getUserId(refreshToken);  // ✅ userId 추출

        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        // 새로운 토큰 발급
        String newAccess = jwtUtil.createJwt("accessToken", username, role, userId, 3600_000L);      // 1시간
        long refreshExpirationMs = 14L * 24 * 3600_000L; // 14일 (밀리초)
        String newRefresh = jwtUtil.createJwt("refreshToken", username, role, userId, refreshExpirationMs);

        //Refresh 토큰 저장 DB에 기존의 Refresh 토큰 삭제 후 새 Refresh 토큰 저장
        repository.deleteByRefresh(refreshToken);
        addRefreshEntity(username, newRefresh, refreshExpirationMs);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", newAccess);
        tokens.put("refreshToken", newRefresh);

        return ResponseEntity.ok(tokens);
    }

    private void addRefreshEntity(String username, String refresh, Long expiredMs) {
        Instant expiresAtInstant = Instant.now().plusMillis(expiredMs);
        String expiresAt = expiresAtInstant.toString();
        RefreshEntity refreshEntity = new RefreshEntity(username, refresh, expiresAt);
        repository.save(refreshEntity);
    }

}
