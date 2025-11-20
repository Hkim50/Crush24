package com.crushai.crushai.service;

import com.crushai.crushai.dto.NearbyUserDto;
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

    private static final String USER_LOCATION_KEY = "user_locations";
    private static final int DEFAULT_SEARCH_LIMIT = 100; // 최대 검색 결과 개수

    public UserLocationService(@Qualifier("geoRedisTemplate") RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 유저 위치 저장 (동일 userId이면 업데이트)
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

            // Redis GEOADD (이미 있으면 자동 업데이트)
            Long result = geoOps.add(
                    USER_LOCATION_KEY,
                    new Point(longitude, latitude),
                    memberName
            );

            // result: null이 아니면 성공
            // 1 = 새로 추가, 0 = 업데이트
            if (result != null) {
                log.info("User location saved: userId={}, lon={}, lat={}, isNew={}", 
                    userId, longitude, latitude, result == 1);
            } else {
                log.warn("Failed to save location for user: {}", userId);
                throw new RuntimeException("Failed to save user location");
            }

        } catch (Exception e) {
            log.error("Error saving user location for userId: {}", userId, e);
            throw new RuntimeException("Failed to save user location: " + e.getMessage(), e);
        }
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
