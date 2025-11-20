package com.crushai.crushai.service;

import com.crushai.crushai.entity.DeviceToken;
import com.crushai.crushai.entity.DeviceType;
import com.crushai.crushai.entity.TokenStatus;
import com.crushai.crushai.repository.DeviceTokenRepository;
import com.crushai.crushai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 디바이스 토큰 관리 서비스
 * APNs(Apple Push Notification service) 토큰 등록/삭제 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {
    
    private final DeviceTokenRepository deviceTokenRepository;
    private final UserRepository userRepository;
    
    /**
     * APNs 토큰 등록 (멱등성 보장)
     */
    @Transactional
    public void registerApnsToken(Long userId, String deviceToken, 
                                   String deviceModel, String osVersion, String appVersion) {
        log.info("Registering APNs token for user: {}", userId);
        
        // 사용자 존재 확인
        if (!userRepository.existsById(userId)) {
            throw new IllegalArgumentException("User not found: " + userId);
        }
        
        // 토큰 정규화 (공백 제거)
        String normalizedToken = deviceToken.replaceAll("\\s+", "");
        
        // 이미 존재하는 토큰인지 확인
        Optional<DeviceToken> existing = deviceTokenRepository.findByDeviceToken(normalizedToken);
        
        if (existing.isPresent()) {
            DeviceToken token = existing.get();
            
            // 다른 사용자의 토큰이면 해당 사용자의 토큰으로 이전
            if (!token.getUserId().equals(userId)) {
                log.info("Token ownership changed: {} -> {}", token.getUserId(), userId);
                token.expire(); // 기존 토큰 만료
                
                // 새 토큰 생성
                createNewToken(userId, normalizedToken, deviceModel, osVersion, appVersion);
            } else {
                // 같은 사용자의 토큰이면 재활성화
                token.activate();
                token.updateDeviceInfo(deviceModel, osVersion, appVersion);
                log.info("Token reactivated for user: {}", userId);
            }
        } else {
            // 새 토큰 생성
            createNewToken(userId, normalizedToken, deviceModel, osVersion, appVersion);
        }
    }
    
    /**
     * 새 토큰 생성
     */
    private void createNewToken(Long userId, String deviceToken,
                                String deviceModel, String osVersion, String appVersion) {
        DeviceToken newToken = DeviceToken.builder()
            .userId(userId)
            .deviceToken(deviceToken)
            .deviceType(DeviceType.IOS)
            .status(TokenStatus.ACTIVE)
            .deviceModel(deviceModel)
            .osVersion(osVersion)
            .appVersion(appVersion)
            .lastUsedAt(Instant.now())
            .failureCount(0)
            .build();
        
        deviceTokenRepository.save(newToken);
        log.info("New token created for user: {}", userId);
    }
    
    /**
     * 로그아웃 시 특정 토큰 비활성화
     */
    @Transactional
    public void deactivateToken(Long userId, String deviceToken) {
        String normalizedToken = deviceToken.replaceAll("\\s+", "");
        
        deviceTokenRepository.findByDeviceToken(normalizedToken)
            .ifPresent(token -> {
                if (token.getUserId().equals(userId)) {
                    token.expire();
                    log.info("Token deactivated for user: {}", userId);
                }
            });
    }
    
    /**
     * 사용자의 모든 토큰 비활성화
     */
    @Transactional
    public void deactivateAllUserTokens(Long userId) {
        List<DeviceToken> tokens = deviceTokenRepository.findByUserIdAndStatus(
            userId, TokenStatus.ACTIVE
        );
        
        tokens.forEach(DeviceToken::expire);
        log.info("Deactivated {} tokens for user: {}", tokens.size(), userId);
    }
    
    /**
     * 사용자의 활성 토큰 조회 (푸시 전송용)
     */
    @Transactional(readOnly = true)
    public List<String> getActiveTokens(Long userId) {
        return deviceTokenRepository.findByUserIdAndStatus(userId, TokenStatus.ACTIVE)
            .stream()
            .map(DeviceToken::getDeviceToken)
            .toList();
    }
    
    /**
     * 토큰 실패 기록
     */
    @Transactional
    public void recordTokenFailure(String deviceToken, String reason) {
        String normalizedToken = deviceToken.replaceAll("\\s+", "");
        
        deviceTokenRepository.findByDeviceToken(normalizedToken)
            .ifPresent(token -> {
                token.recordFailure(reason);
                log.warn("Token failure recorded: {} - {}", maskToken(normalizedToken), reason);
            });
    }
    
    /**
     * 토큰 사용 성공 기록
     */
    @Transactional
    public void recordTokenSuccess(String deviceToken) {
        String normalizedToken = deviceToken.replaceAll("\\s+", "");
        
        deviceTokenRepository.findByDeviceToken(normalizedToken)
            .ifPresent(token -> {
                token.markAsUsed();
                log.debug("Token success recorded: {}", maskToken(normalizedToken));
            });
    }
    
    /**
     * 사용자의 활성 토큰 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean hasActiveTokens(Long userId) {
        return deviceTokenRepository.countByUserIdAndStatus(userId, TokenStatus.ACTIVE) > 0;
    }
    
    /**
     * 토큰 마스킹 (로그용)
     */
    private String maskToken(String token) {
        if (token == null || token.length() <= 8) {
            return "***";
        }
        return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
    }
}
