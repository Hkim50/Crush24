package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 디바이스 토큰 관련 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenResponse {
    
    private String message;
    private boolean success;
    
    public static DeviceTokenResponse success(String message) {
        return new DeviceTokenResponse(message, true);
    }
    
    public static DeviceTokenResponse error(String message) {
        return new DeviceTokenResponse(message, false);
    }
}
