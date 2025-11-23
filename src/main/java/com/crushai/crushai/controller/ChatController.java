package com.crushai.crushai.controller;

import com.crushai.crushai.dto.ChatRoomResponse;
import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.service.ChatRoomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 채팅 관련 API
 * 
 * 범블/틴더 스타일: 채팅방 목록 조회
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatController {

    private final ChatRoomService chatRoomService;

    /**
     * 내 채팅방 목록 조회
     * 
     * GET /api/chat/rooms
     * Authorization: Bearer {JWT}
     * 
     * Response:
     * {
     *   "chatRooms": [
     *     {
     *       "chatRoomId": "uuid-1234",
     *       "matchId": 123,
     *       "matchType": "SWIPE",
     *       "matchedAt": "2024-11-20T10:00:00",
     *       "otherUser": {
     *         "userId": 456,
     *         "name": "홍길동",
     *         "age": 25,
     *         "profilePhoto": "https://..."
     *       },
     *       "lastMessage": "안녕하세요!",
     *       "lastMessageAt": "2024-11-23T15:30:00Z",
     *       "unreadCount": 3
     *     }
     *   ]
     * }
     */
    @GetMapping("/rooms")
    public ResponseEntity<Map<String, Object>> getMyChatRooms(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        
        Long userId = userDetails.getId();
        log.info("Fetching chat rooms for user: {}", userId);
        
        List<ChatRoomResponse> chatRooms = chatRoomService.getMyChatRooms(userId);
        
        return ResponseEntity.ok(Map.of(
                "chatRooms", chatRooms
        ));
    }
}
