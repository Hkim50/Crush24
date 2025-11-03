package com.crushai.crushai.service;

import com.crushai.crushai.entity.UserEntity;
import com.crushai.crushai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 디바이스 토큰 관리 서비스
 * APNs(Apple Push Notification service) 토큰 등록/삭제 처리
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DeviceService {
    
    private final UserRepository userRepository;
    
    /**
     * APNs 디바이스 토큰 등록
     * 
     * @param userId 사용자 ID
     * @param deviceToken APNs 디바이스 토큰
     */
    @Transactional
    public void registerApnsToken(Long userId, String deviceToken) {
        log.info("Registering APNs token for user: {}", userId);
        
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        // 토큰 업데이트
        user.setApnsToken(deviceToken);
        
        // 더티 체킹으로 자동 저장됨 (save 불필요)
        
        log.info("APNs token registered successfully for user: {}", userId);
    }
    
    /**
     * APNs 디바이스 토큰 삭제
     * 로그아웃 시 호출
     * 
     * @param userId 사용자 ID
     */
    @Transactional
    public void removeApnsToken(Long userId) {
        log.info("Removing APNs token for user: {}", userId);
        
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        // 토큰 제거
        user.setApnsToken(null);
        
        // 더티 체킹으로 자동 저장됨 (save 불필요)
        
        log.info("APNs token removed successfully for user: {}", userId);
    }
    
    /**
     * 사용자의 APNs 토큰 조회
     * 
     * @param userId 사용자 ID
     * @return APNs 토큰 (없으면 null)
     */
    @Transactional(readOnly = true)
    public String getApnsToken(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        return user.getApnsToken();
    }
    
    /**
     * APNs 토큰 존재 여부 확인
     * 
     * @param userId 사용자 ID
     * @return 토큰 존재 여부
     */
    @Transactional(readOnly = true)
    public boolean hasApnsToken(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
        
        return user.getApnsToken() != null && !user.getApnsToken().isEmpty();
    }
}
