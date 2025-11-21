package com.crushai.crushai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwipeCardDto {
    private Long userId;
    private String nickname;
    private Integer age;
    private Double distanceKm;  // Redis Geo에서 계산된 거리 (km)
    private String locationName;  // 위치명 (예: "Los Angeles, CA")
    private List<String> photos;
//    private Boolean likedByThem;  // 이 유저가 나를 좋아요 했는지
    
    /**
     * 거리 포맷팅 (UI 표시용)
     * 예: "1.2 km", "5.0 km"
     */
    public String getFormattedDistance() {
        if (distanceKm == null) {
            return "Unknown";
        }
        return String.format("%.1f km", distanceKm);
    }
}
