package com.crushai.crushai.dto;

import jakarta.validation.constraints.NotBlank;
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
    private String deviceToken;
}
