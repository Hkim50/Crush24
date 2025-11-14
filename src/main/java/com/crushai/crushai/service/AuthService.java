package com.crushai.crushai.service;

import com.crushai.crushai.auth.AppleIdTokenValidator;
import com.crushai.crushai.auth.GoogleIdTokenValidator;
import com.crushai.crushai.entity.DeviceType;
import com.crushai.crushai.entity.LoginType;
import com.crushai.crushai.entity.RefreshEntity;
import com.crushai.crushai.entity.Role;
import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.jwt.JWTUtil;
import com.crushai.crushai.repository.RefreshRepository;
import com.crushai.crushai.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshRepository refreshRepository;
    private final JWTUtil jwtUtil;
    private final GoogleIdTokenValidator googleValidator;
    private final AppleIdTokenValidator appleValidator;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public AuthService(UserRepository userRepository, RefreshRepository refreshRepository,
                       JWTUtil jwtUtil, GoogleIdTokenValidator googleValidator,
                       AppleIdTokenValidator appleValidator, RestClient.Builder restClientBuilder, ObjectMapper objectMapper) {
        this.userRepository = userRepository;
        this.refreshRepository = refreshRepository;
        this.jwtUtil = jwtUtil;
        this.googleValidator = googleValidator;
        this.appleValidator = appleValidator;
        this.restClient = restClientBuilder.baseUrl("https://graph.facebook.com").build();
        this.objectMapper = objectMapper;
    }

    public Map<String, String> loginWithGoogle(String idToken, String deviceId, String deviceName) {
        var payload = googleValidator.verify(idToken);
        if (payload == null) {
            throw new RuntimeException("Invalid Google ID Token");
        }

        String email = payload.getEmail();
        String googleId = payload.getSubject();

        UserEntity user = userRepository.findByGoogleId(googleId)
                .or(() -> Optional.ofNullable(email).flatMap(userRepository::findByEmail))
                .orElseGet(() -> userRepository.save(new UserEntity(email, Role.USER, LoginType.GOOGLE, googleId, null)));
 
        boolean isReactivated = false;
        // 탈퇴 상태 확인 후 복구
        if (user.isDelYn()) {
            isReactivated = checkAndReactivateUser(user);
        }

        if (user.getGoogleId() == null) {
            user.setGoogleId(googleId);
            userRepository.save(user);
        }
 
        return generateTokens(user, isReactivated, 
            deviceId != null ? deviceId : "unknown-device",
            DeviceType.IOS,
            deviceName);
    }

    public Map<String, String> loginWithApple(String identityToken, String clientId, String deviceId, String deviceName) {
        var claims = appleValidator.verify(identityToken, clientId);
        if (claims == null) {
            throw new RuntimeException("Invalid Apple ID Token");
        }

        String sub = (String) claims.get("sub");
        String email = (String) claims.get("email");
        System.out.println(claims.toString());

        UserEntity user = userRepository.findByAppleIdSub(sub)
                .or(() -> Optional.ofNullable(email).flatMap(userRepository::findByEmail))
                .orElseGet(() -> userRepository.save(new UserEntity(email, Role.USER, LoginType.APPLE, sub)));
 
        boolean isReactivated = false;
        // 탈퇴 상태 확인 후 복구
        if (user.isDelYn()) {
            isReactivated = checkAndReactivateUser(user);
        }

        if (user.getAppleIdSub() == null) {
            user.setAppleIdSub(sub);
            userRepository.save(user);
        }
 
        return generateTokens(user, isReactivated,
            deviceId != null ? deviceId : "unknown-device",
            DeviceType.IOS,
            deviceName);
    }

    public Map<String, String> loginWithFacebook(String accessToken, String deviceId, String deviceName) {
        try {
            String url = "/v20.0/me?fields=id,email&access_token=" + accessToken;

            // 문자열로 먼저 받아오기
            String responseBody = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);

            // JSON 파싱
            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> response = mapper.readValue(responseBody, new TypeReference<>() {});

            if (response == null || !response.containsKey("email")) {
                throw new RuntimeException("Invalid Facebook Token");
            }

            String facebookId = (String) response.get("id");
            String email = (String) response.get("email");

            UserEntity user = userRepository.findByFacebookId(facebookId)
                    .or(() -> Optional.ofNullable(email).flatMap(userRepository::findByEmail))
                    .orElseGet(() -> userRepository.save(new UserEntity(email, Role.USER, LoginType.FACEBOOK, null, null, facebookId)));
 
            boolean isReactivated = false;
            // 탈퇴 상태 확인 후 복구
            if (user.isDelYn()) {
                isReactivated = checkAndReactivateUser(user);
            }

            if (user.getFacebookId() == null) {
                user.setFacebookId(facebookId);
                userRepository.save(user);
            }
 
            return generateTokens(user, isReactivated,
                deviceId != null ? deviceId : "unknown-device",
                DeviceType.IOS,
                deviceName);

        } catch (RestClientResponseException e) {
            String responseBody = e.getResponseBodyAsString();
            throw new RuntimeException("Facebook API call failed with status " + e.getStatusCode() + ": " + responseBody, e);
        } catch (Exception e) {
            throw new RuntimeException("An unexpected error occurred during Facebook login.", e);
        }
    }

    /**
     * 로그인 시 JWT 토큰 생성 및 Refresh Token 저장
     * 
     * @param user 로그인한 사용자
     * @param isReactivated 복귀 유저 여부
     * @param deviceId 디바이스 고유 ID
     * @param deviceType 디바이스 타입
     * @param deviceName 디바이스 이름
     * @return access token, refresh token 포함된 Map
     */
    private Map<String, String> generateTokens(UserEntity user, boolean isReactivated, 
                                               String deviceId, DeviceType deviceType, String deviceName) {
        long accessExpirationMs = 3600_000L;  // 1시간
        long refreshExpirationMs = 14L * 24 * 3600_000L; // 14일
        
        String accessToken = jwtUtil.createJwt("accessToken", user.getEmail(), 
            user.getRole().name(), user.getId(), accessExpirationMs);
        String refreshToken = jwtUtil.createJwt("refreshToken", user.getEmail(), 
            user.getRole().name(), user.getId(), refreshExpirationMs);

        // Refresh Token 해시 저장
        String tokenHash = hashToken(refreshToken);
        Instant expiresAt = Instant.now().plusMillis(refreshExpirationMs);
        
        RefreshEntity refreshEntity = RefreshEntity.builder()
            .user(user)
            .tokenHash(tokenHash)
            .deviceId(deviceId)
            .deviceType(deviceType)
            .deviceName(deviceName)
            .expiresAt(expiresAt)
            .lastUsedAt(Instant.now())
            .build();
        
        refreshRepository.save(refreshEntity);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("accessToken", accessToken);
        tokens.put("refreshToken", refreshToken);
        tokens.put("onboardingCompleted", String.valueOf(user.isOnboardingCompleted()));
        tokens.put("isReactivated", String.valueOf(isReactivated));
        
        return tokens;
    }
    
    /**
     * Refresh Token을 SHA-256으로 해시
     */
    private String hashToken(String token) {
        try {
            java.security.MessageDigest digest = 
                java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }

    private boolean checkAndReactivateUser(UserEntity user) {
        Instant deletedAt = user.getDeletedAt();

        if (!Instant.now().isAfter(deletedAt)) {
            user.reactivateUser();
            return true; // 복구 성공
        } else {
            throw new RuntimeException("탈퇴 후 30일이 지나 로그인할 수 없습니다.");
        }
    }

}
