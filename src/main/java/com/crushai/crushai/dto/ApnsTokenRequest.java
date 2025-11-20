package com.crushai.crushai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * APNs 디바이스 토큰 등록 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApnsTokenRequest {
    
    @NotBlank(message = "Device token is required")
    @Size(min = 64, max = 200, message = "Invalid device token length")
    private String deviceToken;
    
    @Size(max = 100)
    private String deviceModel; // "iPhone 14 Pro"
    
    @Size(max = 50)
    private String osVersion; // "iOS 17.2"
    
    @Size(max = 50)
    private String appVersion; // "1.0.5"
}
