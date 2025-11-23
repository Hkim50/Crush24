package com.crushai.crushai.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatServiceClient {

    private final WebClient.Builder webClientBuilder;
    
    @Value("${chat.service.url}")
    private String chatServiceUrl;
    
    /**
     * 채팅방 생성 요청 (UUID 지정)
     */
    public String createChatRoomWithId(String chatRoomId, Long user1Id, Long user2Id, Long matchId) {
        log.info("Creating chat room with ID: {}, match: {}, users: {} <-> {}", 
                 chatRoomId, matchId, user1Id, user2Id);
        
        Map<String, Object> request = new HashMap<>();
        request.put("chatRoomId", chatRoomId);  // UUID 전달
        request.put("user1Id", user1Id.toString());
        request.put("user2Id", user2Id.toString());
        request.put("matchId", matchId);
        request.put("matchType", "SWIPE");
        
        try {
            ChatRoomResponse response = webClientBuilder.build()
                .post()
                .uri(chatServiceUrl + "/api/chat/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(ChatRoomResponse.class)
                .timeout(Duration.ofSeconds(5))
                .block();
            
            if (response != null && response.getId() != null) {
                log.info("Chat room created successfully: {}", response.getId());
                return response.getId();
            }
            
            throw new RuntimeException("Chat room ID is null");
            
        } catch (Exception e) {
            log.error("Failed to create chat room", e);
            throw new RuntimeException("Failed to create chat room: " + e.getMessage(), e);
        }
    }

    /**
     * 유저 배치 삭제 요청
     * 
     * 채팅 프로젝트에 유저 삭제 요청을 보내 채팅방 및 메시지 삭제
     * 
     * @param userIds 삭제할 유저 ID 목록
     * @return 삭제 성공 여부
     */
    @SuppressWarnings("unchecked")
    public boolean batchDeleteUsers(List<Long> userIds) {
        try {
            log.info("Requesting chat service to delete {} users", userIds.size());
            
            Map<String, Object> request = new HashMap<>();
            request.put("userIds", userIds);
            
            Map<String, Object> response = webClientBuilder.build()
                    .post()
                    .uri(chatServiceUrl + "/api/users/batch-delete")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))  // 배치 삭제는 시간이 걸릴 수 있음
                    .block();
            
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                log.info("Chat service successfully deleted data for {} users. " +
                        "Deleted {} chat rooms and {} messages",
                        userIds.size(),
                        response.get("deletedChatRooms"),
                        response.get("deletedMessages"));
                return true;
            } else {
                log.warn("Chat service returned unsuccessful response: {}", response);
                return false;
            }
            
        } catch (Exception e) {
            log.error("Failed to delete users in chat service: {}", e.getMessage(), e);
            // 채팅 서비스 오류가 메인 프로세스를 막지 않도록 false 반환
            return false;
        }
    }

    /**
     * 여러 채팅방의 정보 일괄 조회 (내부 API)
     * 
     * @param chatRoomIds 조회할 채팅방 ID 목록
     * @return chatRoomId -> ChatRoomInfo 맵
     */
    @SuppressWarnings("unchecked")
    public Map<String, ChatRoomInfo> getBatchChatRoomInfo(List<String> chatRoomIds) {
        try {
            if (chatRoomIds == null || chatRoomIds.isEmpty()) {
                return Map.of();
            }

            log.info("Requesting chat service for {} chat rooms info", chatRoomIds.size());
            
            Map<String, Object> request = new HashMap<>();
            request.put("chatRoomIds", chatRoomIds);
            
            Map<String, Map<String, Object>> response = webClientBuilder.build()
                    .post()
                    .uri(chatServiceUrl + "/api/internal/chat/rooms/batch-info")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("X-Internal-API-Key", "${internal.api.key:default-secret-key}")  // TODO: 환경변수로 이동
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();
            
            if (response == null) {
                log.warn("Chat service returned null response");
                return Map.of();
            }

            // Map<String, Object> -> Map<String, ChatRoomInfo> 변환
            Map<String, ChatRoomInfo> result = new HashMap<>();
            response.forEach((chatRoomId, infoMap) -> {
                ChatRoomInfo info = ChatRoomInfo.builder()
                        .lastMessage((String) infoMap.get("lastMessage"))
                        .lastMessageAt((String) infoMap.get("lastMessageAt"))
                        .unreadCount((Integer) infoMap.getOrDefault("unreadCount", 0))
                        .build();
                result.put(chatRoomId, info);
            });
            
            log.info("Received info for {} chat rooms", result.size());
            return result;
            
        } catch (Exception e) {
            log.error("Failed to get batch chat room info: {}", e.getMessage(), e);
            return Map.of();
        }
    }

    
    /**
     * 채팅 서버 응답 DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatRoomResponse {
        private String id;  // ChatRoom의 실제 필드명
        private String user1Id;
        private String user2Id;
        private Long matchId;
        private String matchType;
        private String createdAt;
        private Boolean isActive;
    }

    /**
     * 채팅방 정보 DTO
     */
    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatRoomInfo {
        private String lastMessage;
        private String lastMessageAt;
        private Integer unreadCount;
    }
}
