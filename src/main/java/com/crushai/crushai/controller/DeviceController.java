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
     * @param userDetails 인증된 사용자 정보
     * @param request APNs 토큰 요청 DTO
     * @return 등록 결과
     * 
     * POST /api/device/apns-token
     * Request Body: { "deviceToken": "abc123..." }
     * Response: { "message": "Device token registered", "success": true }
     */
    @PostMapping("/apns-token")
    public ResponseEntity<DeviceTokenResponse> registerApnsToken(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody ApnsTokenRequest request
    ) {
        Long userId = userDetails.getUserId();
        
        try {
            deviceService.registerApnsToken(userId, request.getDeviceToken());
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
     * Response: { "message": "Device token removed", "success": true }
     */
    @DeleteMapping("/apns-token")
    public ResponseEntity<DeviceTokenResponse> removeApnsToken(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        
        try {
            deviceService.removeApnsToken(userId);
            return ResponseEntity.ok(
                DeviceTokenResponse.success("Device token removed successfully")
            );
        } catch (IllegalArgumentException e) {
            log.error("Failed to remove APNs token for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest().body(
                DeviceTokenResponse.error(e.getMessage())
            );
        } catch (Exception e) {
            log.error("Unexpected error while removing APNs token for user {}", userId, e);
            return ResponseEntity.internalServerError().body(
                DeviceTokenResponse.error("Failed to remove device token")
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
     * Response: { "message": "Token exists", "success": true }
     */
    @GetMapping("/apns-token/status")
    public ResponseEntity<DeviceTokenResponse> checkApnsTokenStatus(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Long userId = userDetails.getUserId();
        
        try {
            boolean hasToken = deviceService.hasApnsToken(userId);
            
            if (hasToken) {
                return ResponseEntity.ok(
                    DeviceTokenResponse.success("Device token exists")
                );
            } else {
                return ResponseEntity.ok(
                    DeviceTokenResponse.success("No device token registered")
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
