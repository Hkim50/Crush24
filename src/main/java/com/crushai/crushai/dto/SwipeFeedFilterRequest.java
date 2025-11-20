package com.crushai.crushai.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 스와이프 피드 필터 요청 DTO
 * 
 * 사용 예시:
 * {
 *   "minAge": 20,
 *   "maxAge": 25,
 *   "minDistanceKm": 1.0,
 *   "maxDistanceKm": 20.0
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SwipeFeedFilterRequest {
    
    /**
     * 최소 나이 (18-100)
     */
    @NotNull(message = "Minimum age is required")
    @Min(value = 18, message = "Minimum age must be at least 18")
    @Max(value = 100, message = "Minimum age must be at most 100")
    private Integer minAge;
    
    /**
     * 최대 나이 (18-100)
     */
    @NotNull(message = "Maximum age is required")
    @Min(value = 18, message = "Maximum age must be at least 18")
    @Max(value = 100, message = "Maximum age must be at most 100")
    private Integer maxAge;
    
    /**
     * 최소 거리 (km, 1-50)
     */
    @NotNull(message = "Minimum distance is required")
    @Min(value = 1, message = "Minimum distance must be at least 1 km")
    @Max(value = 50, message = "Minimum distance must be at most 50 km")
    private Double minDistanceKm;
    
    /**
     * 최대 거리 (km, 1-50)
     */
    @NotNull(message = "Maximum distance is required")
    @Min(value = 1, message = "Maximum distance must be at least 1 km")
    @Max(value = 50, message = "Maximum distance must be at most 50 km")
    private Double maxDistanceKm;
    
    /**
     * 필터 유효성 검증
     */
    public void validate() {
        if (minAge > maxAge) {
            throw new IllegalArgumentException("Minimum age cannot be greater than maximum age");
        }
        
        if (minDistanceKm > maxDistanceKm) {
            throw new IllegalArgumentException("Minimum distance cannot be greater than maximum distance");
        }
    }
}
