package com.crushai.crushai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Nominatim (OpenStreetMap) Reverse Geocoding Service
 * 좌표 → 위치명 변환
 */
@Service
@Slf4j
public class NominatimService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final RateLimiter rateLimiter;

    @Value("${nominatim.user-agent:CrushApp/1.0 (contact@crush.com)}")
    private String userAgent;

    private static final String NOMINATIM_API_URL = "https://nominatim.openstreetmap.org/reverse";

    public NominatimService(RestClient restClient, ObjectMapper objectMapper) {
        this.restClient = restClient;
        this.objectMapper = objectMapper;
        // Nominatim Rate Limit: 1 request/sec
        this.rateLimiter = RateLimiter.create(1.0);
    }

    /**
     * 좌표를 "City, State" 형식의 위치명으로 변환
     * 
     * @param latitude 위도
     * @param longitude 경도
     * @return "Los Angeles, CA" 형식의 위치명, 실패 시 null
     */
    public String getLocationName(double latitude, double longitude) {
        try {
            // Rate Limiting (1 req/sec)
            rateLimiter.acquire();
            
            // API 호출
            String url = String.format(
                "%s?lat=%s&lon=%s&format=json&addressdetails=1",
                NOMINATIM_API_URL,
                latitude,
                longitude
            );
            
            String response = restClient.get()
                .uri(url)
                .header("User-Agent", userAgent)
                .retrieve()
                .body(String.class);
            
            if (response != null) {
                return parseLocationName(response);
            }
            
            log.warn("Nominatim API returned null response");
            return null;
            
        } catch (Exception e) {
            log.error("Failed to get location name for ({}, {}): {}", 
                     latitude, longitude, e.getMessage());
            return null;
        }
    }

    /**
     * Nominatim 응답을 "City, State" 형식으로 파싱
     * 
     * @param jsonResponse Nominatim API JSON 응답
     * @return "Los Angeles, CA" 형식, 파싱 실패 시 null
     */
    private String parseLocationName(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode address = root.path("address");
            
            if (address.isMissingNode()) {
                log.warn("No address field in Nominatim response");
                return null;
            }
            
            // City 추출 (우선순위: city > town > village > county)
            String city = null;
            if (address.has("city")) {
                city = address.get("city").asText();
            } else if (address.has("town")) {
                city = address.get("town").asText();
            } else if (address.has("village")) {
                city = address.get("village").asText();
            } else if (address.has("county")) {
                city = address.get("county").asText();
            }
            
            // State 추출
            String state = address.has("state") ? address.get("state").asText() : null;
            
            // State 약자 변환 (예: California → CA)
            if (state != null) {
                state = convertStateToAbbreviation(state);
            }
            
            // "City, State" 형식으로 반환
            if (city != null && state != null) {
                return city + ", " + state;
            } else if (city != null) {
                return city;
            }
            
            log.warn("Could not extract city/state from Nominatim response");
            return null;
            
        } catch (Exception e) {
            log.error("Failed to parse Nominatim response: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 미국 주 이름을 약자로 변환
     * 
     * @param stateName 주 전체 이름 (예: "California")
     * @return 주 약자 (예: "CA"), 매칭 안 되면 원본 반환
     */
    private String convertStateToAbbreviation(String stateName) {
        // 주요 주 약자 매핑
        return switch (stateName.toLowerCase()) {
            case "california" -> "CA";
            case "new york" -> "NY";
            case "texas" -> "TX";
            case "florida" -> "FL";
            case "illinois" -> "IL";
            case "pennsylvania" -> "PA";
            case "ohio" -> "OH";
            case "georgia" -> "GA";
            case "north carolina" -> "NC";
            case "michigan" -> "MI";
            case "new jersey" -> "NJ";
            case "virginia" -> "VA";
            case "washington" -> "WA";
            case "arizona" -> "AZ";
            case "massachusetts" -> "MA";
            case "tennessee" -> "TN";
            case "indiana" -> "IN";
            case "missouri" -> "MO";
            case "maryland" -> "MD";
            case "wisconsin" -> "WI";
            case "colorado" -> "CO";
            case "minnesota" -> "MN";
            case "south carolina" -> "SC";
            case "alabama" -> "AL";
            case "louisiana" -> "LA";
            case "kentucky" -> "KY";
            case "oregon" -> "OR";
            case "oklahoma" -> "OK";
            case "connecticut" -> "CT";
            case "utah" -> "UT";
            case "iowa" -> "IA";
            case "nevada" -> "NV";
            case "arkansas" -> "AR";
            case "mississippi" -> "MS";
            case "kansas" -> "KS";
            case "new mexico" -> "NM";
            case "nebraska" -> "NE";
            case "west virginia" -> "WV";
            case "idaho" -> "ID";
            case "hawaii" -> "HI";
            case "new hampshire" -> "NH";
            case "maine" -> "ME";
            case "montana" -> "MT";
            case "rhode island" -> "RI";
            case "delaware" -> "DE";
            case "south dakota" -> "SD";
            case "north dakota" -> "ND";
            case "alaska" -> "AK";
            case "vermont" -> "VT";
            case "wyoming" -> "WY";
            default -> stateName; // 매칭 안 되면 원본 반환
        };
    }
}
