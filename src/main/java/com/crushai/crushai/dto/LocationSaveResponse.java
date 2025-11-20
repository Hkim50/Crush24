package com.crushai.crushai.dto;

import java.time.Instant;

/**
 * 위치 저장 응답 DTO
 */
public record LocationSaveResponse(
        String message,
        Long userId,
        double longitude,
        double latitude,
        Instant timestamp
) {
    public static LocationSaveResponse success(Long userId, double longitude, double latitude) {
        return new LocationSaveResponse(
                "Location saved successfully",
                userId,
                longitude,
                latitude,
                Instant.now()
        );
    }
}
