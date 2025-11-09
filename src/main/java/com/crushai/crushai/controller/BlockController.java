package com.crushai.crushai.controller;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.ReportResponse;
import com.crushai.crushai.service.BlockService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 사용자 차단 관련 API 컨트롤러
 */
@RestController
@RequestMapping("/api/block")
@RequiredArgsConstructor
@Slf4j
public class BlockController {
    
    private final BlockService blockService;
    
    /**
     * 사용자 차단
     * POST /api/block/{userId}
     * 
     * Response (Success - 200):
     * {
     *   "success": true,
     *   "message": "사용자를 차단했습니다"
     * }
     * 
     * Response (Error - 400):
     * {
     *   "success": false,
     *   "message": "자기 자신을 차단할 수 없습니다"
     * }
     * 
     * Response (Error - 409):
     * {
     *   "success": false,
     *   "message": "이미 차단한 사용자입니다"
     * }
     */
    @PostMapping("/{userId}")
    public ResponseEntity<ReportResponse> blockUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userId
    ) {
        Long blockerId = userDetails.getUserId();
        log.info("User {} attempting to block user {}", blockerId, userId);
        
        try {
            blockService.blockUser(blockerId, userId);
            return ResponseEntity.ok(ReportResponse.success("사용자를 차단했습니다"));
            
        } catch (IllegalArgumentException e) {
            log.warn("Invalid block request: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ReportResponse.failure(e.getMessage()));
                    
        } catch (IllegalStateException e) {
            log.warn("Duplicate block attempt: {}", e.getMessage());
            return ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(ReportResponse.failure(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Unexpected error while blocking user", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ReportResponse.failure("차단 처리 중 오류가 발생했습니다"));
        }
    }
    
    /**
     * 차단 해제
     * DELETE /api/block/{userId}
     * 
     * Response (Success - 200):
     * {
     *   "success": true,
     *   "message": "차단을 해제했습니다"
     * }
     * 
     * Response (Error - 400):
     * {
     *   "success": false,
     *   "message": "차단하지 않은 사용자입니다"
     * }
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<ReportResponse> unblockUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long userId
    ) {
        Long blockerId = userDetails.getUserId();
        log.info("User {} attempting to unblock user {}", blockerId, userId);
        
        try {
            blockService.unblockUser(blockerId, userId);
            return ResponseEntity.ok(ReportResponse.success("차단을 해제했습니다"));
            
        } catch (IllegalStateException e) {
            log.warn("Invalid unblock request: {}", e.getMessage());
            return ResponseEntity
                    .badRequest()
                    .body(ReportResponse.failure(e.getMessage()));
                    
        } catch (Exception e) {
            log.error("Unexpected error while unblocking user", e);
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ReportResponse.failure("차단 해제 중 오류가 발생했습니다"));
        }
    }
}
