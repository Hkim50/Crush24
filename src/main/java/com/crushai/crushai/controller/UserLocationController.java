package com.crushai.crushai.controller;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.LocationSaveResponse;
import com.crushai.crushai.dto.NearbyUserDto;
import com.crushai.crushai.dto.UserLocationRequest;
import com.crushai.crushai.service.UserLocationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/location")
@Validated
@Slf4j
public class UserLocationController {

    private final UserLocationService userLocationService;

    public UserLocationController(UserLocationService userLocationService) {
        this.userLocationService = userLocationService;
    }

    /**
     * 사용자 위치 저장 (비동기 처리)
     * 
     * Fire-and-forget 방식: 즉시 202 Accepted 응답 반환
     * 실제 위치 저장 및 위치명 업데이트는 백그라운드에서 처리
     * 
     * @param customUserDetails 인증된 사용자 정보
     * @param userLocationRequest 위치 정보 (경도, 위도)
     * @return 202 Accepted (처리 중)
     */
    @PostMapping("/save")
    public ResponseEntity<LocationSaveResponse> saveUserLocation(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestBody @Valid UserLocationRequest userLocationRequest) {
        
        Long userId = customUserDetails.getUserId();
        
        log.info("Received location update for userId: {}, lon: {}, lat: {}", 
            userId, userLocationRequest.longitude(), userLocationRequest.latitude());
        
        // 비동기로 위치 저장 (@Async)
        userLocationService.saveUserLocationAsync(
            userId, 
            userLocationRequest.longitude(), 
            userLocationRequest.latitude()
        );

        // 즉시 202 Accepted 응답 반환
        return ResponseEntity.accepted()
            .body(LocationSaveResponse.accepted(
                userId,
                userLocationRequest.longitude(),
                userLocationRequest.latitude()
            ));
    }

    /**
     * 반경 내 주변 사용자 검색
     * 
     * @param customUserDetails 인증된 사용자 정보
     * @param radiusKm 검색 반경 (km, 기본값: 5km, 최대: 50km)
     * @return 주변 사용자 목록 (거리순 정렬)
     */
    @GetMapping("/nearby")
    public ResponseEntity<List<NearbyUserDto>> getNearbyUsers(
            @AuthenticationPrincipal CustomUserDetails customUserDetails,
            @RequestParam(defaultValue = "5.0")
            @Min(value = 1, message = "반경은 최소 1km 이상이어야 합니다")
            @Max(value = 50, message = "반경은 최대 50km 이하여야 합니다")
            double radiusKm) {

        Long userId = customUserDetails.getUserId();

        log.info("Searching nearby users for userId: {}, radius: {}km", userId, radiusKm);

        List<NearbyUserDto> nearbyUsers = userLocationService.getUsersWithinRadius(userId, radiusKm);

        return ResponseEntity.ok(nearbyUsers);
    }

    /**
     * 사용자 위치 삭제 (탈퇴 시)
     * 
     * @param customUserDetails 인증된 사용자 정보
     * @return 삭제 결과
     */
    @DeleteMapping("/delete")
    public ResponseEntity<Void> deleteUserLocation(
            @AuthenticationPrincipal CustomUserDetails customUserDetails) {
        
        Long userId = customUserDetails.getUserId();
        
        log.info("Deleting location for userId: {}", userId);
        
        userLocationService.deleteUserLocation(userId);
        
        return ResponseEntity.noContent().build();
    }
}
