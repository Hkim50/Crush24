package com.crushai.crushai.service;

import com.crushai.crushai.entity.UserBlock;
import com.crushai.crushai.repository.UserBlockRepository;
import com.crushai.crushai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 차단 기능 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockService {
    
    private final UserBlockRepository blockRepository;
    private final UserRepository userRepository;
    
    /**
     * 사용자 차단
     */
    @Transactional
    public void blockUser(Long blockerId, Long blockedUserId) {
        log.info("User {} blocking user {}", blockerId, blockedUserId);
        
        // 1. 자기 자신 차단 방지
        if (blockerId.equals(blockedUserId)) {
            throw new IllegalArgumentException("자기 자신을 차단할 수 없습니다");
        }
        
        // 2. 차단 대상 존재 확인
        if (!userRepository.existsById(blockedUserId)) {
            throw new IllegalArgumentException("차단할 사용자를 찾을 수 없습니다");
        }
        
        // 3. 이미 차단했는지 확인
        if (blockRepository.existsByBlockerIdAndBlockedUserId(blockerId, blockedUserId)) {
            throw new IllegalStateException("이미 차단한 사용자입니다");
        }
        
        // 4. 차단 생성
        UserBlock block = UserBlock.builder()
                .blockerId(blockerId)
                .blockedUserId(blockedUserId)
                .build();
        
        blockRepository.save(block);
        
        log.info("User {} successfully blocked user {}", blockerId, blockedUserId);
    }
    
    /**
     * 차단 해제
     */
    @Transactional
    public void unblockUser(Long blockerId, Long blockedUserId) {
        log.info("User {} unblocking user {}", blockerId, blockedUserId);
        
        if (!blockRepository.existsByBlockerIdAndBlockedUserId(blockerId, blockedUserId)) {
            throw new IllegalStateException("차단하지 않은 사용자입니다");
        }
        
        blockRepository.deleteByBlockerIdAndBlockedUserId(blockerId, blockedUserId);
        
        log.info("User {} successfully unblocked user {}", blockerId, blockedUserId);
    }
}
