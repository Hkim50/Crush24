package com.crushai.crushai.controller;

import com.crushai.crushai.dto.ApnsTokenRequest;
import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.DeviceTokenResponse;
import com.crushai.crushai.service.DeviceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 디바이스 토큰 관리 컨트롤러
 * iOS 푸시 알림을 위한 APNs 토큰 등록/삭제 API
 */
@RestController
@RequestMapping("/api/device")
@RequiredArgsConstructor
@Slf4j
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * APNs 디바이스 토큰 등록
     * 
     * iOS 앱에서 APNs 토큰을 받으면 즉시 호출해야 함
     * - 앱 첫 실행 시
     * - 로그인 성공 시
     * - 토큰이 갱신되었을 때
     * 
     * @param userDetails 인증된 사용자 정보
     * @param request APNs 토큰 요청 DTO (deviceToken 필수, 나머지 선택)
     * @return 등록 결과
     * 
     * POST /api/device/apns-token
     * Request Body: 
     * {
     *   "deviceToken": "abc123...",
     *   "deviceModel": "iPhone 14 Pro",  // 선택
     *   "osVersion": "iOS 17.2",          // 선택
     *   "appVersion": "1.0.5"             // 선택
     * }
     * Response: { "message": "Device token registered successfully", "success": true }
     */
    @PostMapping("/apns-token")
    public ResponseEntity<DeviceTokenResponse> registerApnsToken(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ApnsTokenRequest request
    ) {
        Long userId = userDetails.getUserId();
        
        try {
            deviceService.registerApnsToken(
                userId, 
                request.getDeviceToken(),
                request.getDeviceModel(),
                request.getOsVersion(),
                request.getAppVersion()
            );
            
            log.info("APNs token registered for user: {}", userId);
            
            return ResponseEntity.ok(
                DeviceTokenResponse.success("Device token registered successfully")
            );
        } catch (IllegalArgumentException e) {
            log.error("Failed to register APNs token for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DeviceTokenResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Unexpected error while registering APNs token for user {}", userId, e);
            return ResponseEntity.internalServerError().body(
                DeviceTokenResponse.error("Failed to register device token")
            );
        }
    }
    
    /**
     * APNs 디바이스 토큰 삭제
     * 로그아웃 시 호출하여 더 이상 푸시 알림을 받지 않도록 함
     * 
     * @param userDetails 인증된 사용자 정보
     * @return 삭제 결과
     * 
     * DELETE /api/device/apns-token
     * Response: { "message": "Device token removed successfully", "success": true }
     */
    @DeleteMapping("/apns-token")
    public ResponseEntity<DeviceTokenResponse> removeApnsToken(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        
        try {
            deviceService.deactivateAllUserTokens(userId);
            
            log.info("All APNs tokens removed for user: {}", userId);
            
            return ResponseEntity.ok(
                DeviceTokenResponse.success("All device tokens removed successfully")
            );
        } catch (IllegalArgumentException e) {
            log.error("Failed to remove APNs tokens for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DeviceTokenResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Unexpected error while removing APNs tokens for user {}", userId, e);
            return ResponseEntity.internalServerError().body(
                DeviceTokenResponse.error("Failed to remove device tokens")
            );
        }
    }
    
    /**
     * APNs 디바이스 토큰 존재 여부 확인
     * 
     * @param userDetails 인증된 사용자 정보
     * @return 토큰 존재 여부
     * 
     * GET /api/device/apns-token/status
     * Response: { "message": "Active tokens found", "success": true }
     */
    @GetMapping("/apns-token/status")
    public ResponseEntity<DeviceTokenResponse> checkApnsTokenStatus(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        
        try {
            boolean hasTokens = deviceService.hasActiveTokens(userId);
            
            if (hasTokens) {
                return ResponseEntity.ok(
                    DeviceTokenResponse.success("Active device tokens found")
                );
            } else {
                return ResponseEntity.ok(
                    DeviceTokenResponse.success("No active device tokens")
                );
            }
        } catch (IllegalArgumentException e) {
            log.error("Failed to check APNs token status for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DeviceTokenResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Unexpected error while checking APNs token status for user {}", userId, e);
            return ResponseEntity.internalServerError().body(
                DeviceTokenResponse.error("Failed to check device token status")
            );
        }
    }
}
