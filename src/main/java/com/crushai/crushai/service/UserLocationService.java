package com.crushai.crushai.service;

import com.crushai.crushai.dto.NearbyUserDto;
import com.crushai.crushai.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Slf4j
public class UserLocationService {

    private final RedisTemplate<String, String> redisTemplate;
    private final GeocodingService geocodingService;
    private final UserRepository userRepository;

    private static final String USER_LOCATION_KEY = "user_locations";
    private static final int DEFAULT_SEARCH_LIMIT = 100;
    private static final double LOCATION_NAME_UPDATE_THRESHOLD_KM = 15.0; // 15km

    public UserLocationService(
            @Qualifier("geoRedisTemplate") RedisTemplate<String, String> redisTemplate,
            GeocodingService geocodingService,
            UserRepository userRepository) {
        this.redisTemplate = redisTemplate;
        this.geocodingService = geocodingService;
        this.userRepository = userRepository;
    }

    /**
     * 유저 위치 저장 및 위치명 업데이트
     * 
     * 1. Redis에서 기존 위치 조회
     * 2. 거리 계산 (기존 vs 새 위치)
     * 3. Redis 좌표 업데이트 (항상)
     * 4. 15km 이상 차이나면 locationName 업데이트 (캐시 확인 → API 호출)
     * 
     * @param userId 사용자 ID
     * @param longitude 경도 (-180 ~ 180)
     * @param latitude 위도 (-90 ~ 90)
     * @throws RuntimeException Redis 저장 실패 시
     */
    public void saveUserLocation(Long userId, double longitude, double latitude) {
        try {
            GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
            String memberName = "user:" + userId;

            // 1. 기존 위치 조회
            Point oldLocation = getUserLocation(userId);

            // 2. Redis 좌표 업데이트 (항상)
            Long result = geoOps.add(
                    USER_LOCATION_KEY,
                    new Point(longitude, latitude),
                    memberName
            );

            if (result != null) {
                log.info("User location saved: userId={}, lon={}, lat={}, isNew={}", 
                    userId, longitude, latitude, result == 1);
            } else {
                log.warn("Failed to save location for user: {}", userId);
                throw new RuntimeException("Failed to save user location");
            }

            // 3. 15km 이상 이동 시 locationName 업데이트
            if (oldLocation != null) {
                double distance = calculateDistance(
                    oldLocation.getY(), oldLocation.getX(),
                    latitude, longitude
                );
                
                log.debug("Location distance for userId {}: {}km", userId, distance);
                
                if (distance >= LOCATION_NAME_UPDATE_THRESHOLD_KM) {
                    updateLocationName(userId, latitude, longitude);
                }
            } else {
                // 첫 위치 저장 시 locationName 업데이트
                updateLocationName(userId, latitude, longitude);
            }

        } catch (Exception e) {
            log.error("Error saving user location for userId: {}", userId, e);
            throw new RuntimeException("Failed to save user location: " + e.getMessage(), e);
        }
    }
    
    /**
     * 사용자의 위치명 업데이트 (캐싱 활용)
     * 
     * @param userId 사용자 ID
     * @param latitude 위도
     * @param longitude 경도
     */
    private void updateLocationName(Long userId, double latitude, double longitude) {
        try {
            // GeocodingService를 통해 위치명 조회 (캐싱 포함)
            String locationName = geocodingService.getLocationName(latitude, longitude);
            
            if (locationName != null) {
                // DB 업데이트
                userRepository.findById(userId).ifPresent(user -> {
                    if (user.getUserInfo() != null) {
                        user.getUserInfo().updateLocationName(locationName);
                        userRepository.save(user);
                        log.info("Location name updated: userId={}, locationName={}", userId, locationName);
                    }
                });
            } else {
                log.warn("Failed to get location name for userId: {}", userId);
            }
            
        } catch (Exception e) {
            log.error("Error updating location name for userId: {}", userId, e);
            // locationName 업데이트 실패는 치명적이지 않으므로 예외를 던지지 않음
        }
    }
    
    /**
     * 두 좌표 사이의 거리 계산 (Haversine Formula)
     * 
     * @param lat1 위도1
     * @param lon1 경도1
     * @param lat2 위도2
     * @param lon2 경도2
     * @return 거리 (km)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int EARTH_RADIUS_KM = 6371;
        
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }

    /**
     * 특정 사용자의 위치 정보 삭제
     * 
     * @param userId 사용자 ID
     */
    public void deleteUserLocation(Long userId) {
        try {
            GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
            String memberName = "user:" + userId;
            
            Long removed = geoOps.remove(USER_LOCATION_KEY, memberName);
            
            if (removed != null && removed > 0) {
                log.info("User location deleted: userId={}", userId);
            } else {
                log.warn("No location found to delete for userId: {}", userId);
            }
            
        } catch (Exception e) {
            log.error("Error deleting user location for userId: {}", userId, e);
            throw new RuntimeException("Failed to delete user location: " + e.getMessage(), e);
        }
    }

    /**
     * userId 기준으로 현재 위치를 가져와서, 반경 내 유저 검색
     * 거리 정보와 좌표 포함하여 반환 (가까운 순 정렬)
     *
     * @param userId 중심이 되는 유저 ID (자기 자신 제외)
     * @param radiusKm 검색 반경 (킬로미터)
     * @return 반경 내 유저 정보 리스트 (거리순 정렬, 최대 100명)
     */
    public List<NearbyUserDto> getUsersWithinRadius(Long userId, double radiusKm) {
        return getUsersWithinRadius(userId, radiusKm, DEFAULT_SEARCH_LIMIT);
    }

    /**
     * userId 기준으로 현재 위치를 가져와서, 반경 내 유저 검색 (결과 개수 제한)
     *
     * @param userId 중심이 되는 유저 ID (자기 자신 제외)
     * @param radiusKm 검색 반경 (킬로미터)
     * @param limit 최대 결과 개수
     * @return 반경 내 유저 정보 리스트 (거리순 정렬)
     */
    public List<NearbyUserDto> getUsersWithinRadius(Long userId, double radiusKm, int limit) {
        try {
            GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
            String memberName = "user:" + userId;

            // 1. 유저 현재 위치 조회
            List<Point> points = geoOps.position(USER_LOCATION_KEY, memberName);
            if (points == null || points.isEmpty() || points.get(0) == null) {
                log.debug("No location found for userId: {}", userId);
                return List.of();
            }

            Point userPoint = points.get(0);

            // 2. Circle 객체 생성 (반경 검색)
            Circle circle = new Circle(userPoint, new Distance(radiusKm, Metrics.KILOMETERS));

            // 3. 검색 옵션 설정 (거리 정보 포함, 가까운 순 정렬, 결과 제한)
            RedisGeoCommands.GeoRadiusCommandArgs args =
                    RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                            .includeDistance()      // 거리 정보 포함
                            .includeCoordinates()   // 좌표 정보 포함
                            .sortAscending()        // 가까운 순 정렬
                            .limit(limit);          // 최대 개수 제한

            // 4. 반경 내 유저 검색
            GeoResults<RedisGeoCommands.GeoLocation<String>> results =
                    geoOps.radius(USER_LOCATION_KEY, circle, args);

            if (results == null || results.getContent().isEmpty()) {
                log.debug("No users found within {}km of userId: {}", radiusKm, userId);
                return List.of();
            }

            // 5. 자기 자신 제외 후 DTO 변환
            List<NearbyUserDto> nearbyUsers = results.getContent().stream()
                    .filter(result -> !result.getContent().getName().equals(memberName))
                    .map(result -> {
                        String name = result.getContent().getName();
                        Long uid = Long.valueOf(name.replace("user:", ""));
                        double distance = result.getDistance().getValue(); // km
                        Point point = result.getContent().getPoint();

                        return new NearbyUserDto(
                                uid,
                                distance,
                                point.getX(), // longitude
                                point.getY()  // latitude
                        );
                    })
                    .toList();

            log.info("Found {} users within {}km of userId: {}", nearbyUsers.size(), radiusKm, userId);
            return nearbyUsers;

        } catch (Exception e) {
            log.error("Error searching users within radius for userId: {}", userId, e);
            throw new RuntimeException("Failed to search nearby users: " + e.getMessage(), e);
        }
    }

    /**
     * 특정 사용자의 현재 위치 조회
     * 
     * @param userId 사용자 ID
     * @return Point 객체 (없으면 null)
     */
    public Point getUserLocation(Long userId) {
        try {
            GeoOperations<String, String> geoOps = redisTemplate.opsForGeo();
            String memberName = "user:" + userId;

            List<Point> points = geoOps.position(USER_LOCATION_KEY, memberName);
            
            if (points != null && !points.isEmpty() && points.get(0) != null) {
                return points.get(0);
            }
            
            return null;
            
        } catch (Exception e) {
            log.error("Error getting user location for userId: {}", userId, e);
            return null;
        }
    }
}
