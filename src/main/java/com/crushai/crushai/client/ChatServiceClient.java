package com.crushai.crushai.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class ChatServiceClient {

    private final WebClient.Builder webClientBuilder;
    
    @Value("${chat.service.url}")
    private String chatServiceUrl;
    
    /**
     * 채팅방 생성 요청
     */
    public String createChatRoom(Long user1Id, Long user2Id, Long matchId) {
        log.info("Creating chat room for match: {}, users: {} <-> {}", matchId, user1Id, user2Id);
        
        Map<String, Object> request = new HashMap<>();
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
            
            if (response != null && response.getChatRoomId() != null) {
                log.info("Chat room created successfully: {}", response.getChatRoomId());
                return response.getChatRoomId();
            }
            
            throw new RuntimeException("Chat room ID is null");
            
        } catch (Exception e) {
            log.error("Failed to create chat room", e);
            throw new RuntimeException("Failed to create chat room: " + e.getMessage(), e);
        }
    }
    
    /**
     * 채팅 서버 응답 DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ChatRoomResponse {
        private String chatRoomId;
        private String message;
    }
}
