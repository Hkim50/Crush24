package com.crushai.crushai.dto;

/**
 * 반경 내 주변 사용자 정보 DTO
 */
public record NearbyUserDto(
        Long userId,
        double distanceKm,
        double longitude,
        double latitude
) {
    /**
     * 거리를 포맷팅된 문자열로 반환
     * @return "500m" 또는 "1.2km" 형식
     */
    public String getFormattedDistance() {
        if (distanceKm < 1.0) {
            return String.format("%.0fm", distanceKm * 1000);
        }
        return String.format("%.1fkm", distanceKm);
    }
}
