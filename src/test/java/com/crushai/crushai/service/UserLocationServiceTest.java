package com.crushai.crushai.service;

import com.crushai.crushai.dto.NearbyUserDto;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;

/**
 * UserLocationService 테스트
 * 
 * ⚠️ 사전 준비: Docker로 Redis를 실행해야 합니다.
 * 
 * 실행 방법:
 * 1. Redis 시작: docker-compose up -d redis
 * 2. 테스트 실행: ./gradlew test --tests UserLocationServiceTest
 * 3. Redis 중지: docker-compose down
 */
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserLocationServiceTest {

    @Autowired
    @Qualifier("geoRedisTemplate")
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private UserLocationService userLocationService;

    @BeforeEach
    void cleanData() {
        // 테스트 전 Redis 데이터 초기화
        try {
            redisTemplate.delete("user_locations");
        } catch (Exception e) {
            System.err.println("⚠️ Redis 연결 실패. Docker로 Redis를 시작하세요: docker-compose up -d redis");
            Assumptions.abort("Redis 서버가 필요합니다: " + e.getMessage());
        }
    }

    @Test
    @DisplayName("위치 저장 및 반경 내 검색 - 거리 정보 포함")
    void testSaveAndRadiusSearchWithDistance() {
        // Given: 3명의 유저 위치 저장
        userLocationService.saveUserLocation(1L, 127.0, 37.5);      // 기준 유저 (서울 중심)
        userLocationService.saveUserLocation(2L, 127.01, 37.51);    // 가까운 유저 (~1.5km)
        userLocationService.saveUserLocation(3L, 127.5, 37.8);      // 먼 유저 (~50km)

        // When: 반경 5km 내 검색
        List<NearbyUserDto> nearby = userLocationService.getUsersWithinRadius(1L, 5.0);

        // Then: 2번만 포함, 3번 제외
        Assertions.assertEquals(1, nearby.size());
        Assertions.assertEquals(2L, nearby.get(0).userId());
        Assertions.assertTrue(nearby.get(0).distanceKm() < 5.0);
        
        // 거리 정보 확인
        System.out.println("✅ User 2 distance: " + nearby.get(0).getFormattedDistance());
    }

    @Test
    @DisplayName("거리순 정렬 확인")
    void testDistanceSorting() {
        // Given: 4명의 유저 위치 저장 (거리가 다름)
        userLocationService.saveUserLocation(1L, 127.0, 37.5);      // 기준
        userLocationService.saveUserLocation(2L, 127.02, 37.52);    // 중간
        userLocationService.saveUserLocation(3L, 127.01, 37.51);    // 가까움
        userLocationService.saveUserLocation(4L, 127.03, 37.53);    // 멀음

        // When: 반경 10km 내 검색
        List<NearbyUserDto> nearby = userLocationService.getUsersWithinRadius(1L, 10.0);

        // Then: 가까운 순서로 정렬되어야 함 (3 -> 2 -> 4)
        Assertions.assertEquals(3, nearby.size());
        Assertions.assertEquals(3L, nearby.get(0).userId());  // 가장 가까움
        Assertions.assertTrue(nearby.get(0).distanceKm() < nearby.get(1).distanceKm());
        Assertions.assertTrue(nearby.get(1).distanceKm() < nearby.get(2).distanceKm());
    }

    @Test
    @DisplayName("위치 정보가 없는 유저 검색")
    void testNoLocationForUser() {
        // When: 위치를 등록하지 않은 유저가 검색
        List<NearbyUserDto> nearby = userLocationService.getUsersWithinRadius(999L, 5.0);
        
        // Then: 빈 리스트 반환
        Assertions.assertTrue(nearby.isEmpty());
    }

    @Test
    @DisplayName("위치 업데이트 동작 확인")
    void testUpdateLocation() {
        // Given: 초기 위치 저장
        userLocationService.saveUserLocation(1L, 127.0, 37.5);
        userLocationService.saveUserLocation(2L, 127.01, 37.51);  // 가까운 위치
        
        List<NearbyUserDto> nearbyBefore = userLocationService.getUsersWithinRadius(1L, 5.0);
        Assertions.assertEquals(1, nearbyBefore.size());
        
        // When: 1번 유저가 먼 곳으로 이동
        userLocationService.saveUserLocation(1L, 127.5, 37.8);
        
        // Then: 2번 유저가 반경 밖으로 나감
        List<NearbyUserDto> nearbyAfter = userLocationService.getUsersWithinRadius(1L, 5.0);
        Assertions.assertTrue(nearbyAfter.isEmpty());
    }

    @Test
    @DisplayName("위치 삭제 동작 확인")
    void testDeleteLocation() {
        // Given: 위치 저장
        userLocationService.saveUserLocation(1L, 127.0, 37.5);
        userLocationService.saveUserLocation(2L, 127.01, 37.51);
        
        List<NearbyUserDto> nearbyBefore = userLocationService.getUsersWithinRadius(1L, 5.0);
        Assertions.assertEquals(1, nearbyBefore.size());
        
        // When: 2번 유저 위치 삭제
        userLocationService.deleteUserLocation(2L);
        
        // Then: 2번 유저가 검색되지 않음
        List<NearbyUserDto> nearbyAfter = userLocationService.getUsersWithinRadius(1L, 5.0);
        Assertions.assertTrue(nearbyAfter.isEmpty());
    }

    @Test
    @DisplayName("결과 개수 제한 확인")
    void testSearchLimit() {
        // Given: 기준 유저 + 가까운 위치에 150명 저장
        userLocationService.saveUserLocation(1L, 127.0, 37.5);
        
        for (long i = 2; i <= 151; i++) {
            userLocationService.saveUserLocation(i, 127.001, 37.501);  // 모두 가까운 위치
        }
        
        // When: 반경 5km 내 검색 (기본 제한: 100명)
        List<NearbyUserDto> nearby = userLocationService.getUsersWithinRadius(1L, 5.0);
        
        // Then: 최대 100명만 반환
        Assertions.assertTrue(nearby.size() <= 100);
    }

    @Test
    @DisplayName("자기 자신은 결과에서 제외")
    void testExcludeSelf() {
        // Given: 1명만 저장
        userLocationService.saveUserLocation(1L, 127.0, 37.5);
        
        // When: 자기 자신을 기준으로 검색
        List<NearbyUserDto> nearby = userLocationService.getUsersWithinRadius(1L, 100.0);
        
        // Then: 자기 자신은 포함되지 않음
        Assertions.assertTrue(nearby.isEmpty());
    }
}
