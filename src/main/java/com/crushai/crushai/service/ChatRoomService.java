package com.crushai.crushai.service;

import com.crushai.crushai.client.ChatServiceClient;
import com.crushai.crushai.dto.ChatRoomResponse;
import com.crushai.crushai.entity.Match;
import com.crushai.crushai.entity.UserInfoEntity;
import com.crushai.crushai.repository.MatchRepository;
import com.crushai.crushai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 채팅방 목록 서비스
 * 
 * 메인 프로젝트에서 채팅방 목록을 조회하고 조합
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ChatRoomService {

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;
    private final ChatServiceClient chatServiceClient;

    /**
     * 내 채팅방 목록 조회
     * 
     * 1. 내 활성 매칭 조회 (isActive = true)
     * 2. 채팅 프로젝트에서 마지막 메시지 정보 조회
     * 3. 상대방 프로필 정보 조회
     * 4. 조합하여 반환
     * 
     * @param myUserId 내 유저 ID
     * @return 채팅방 목록
     */
    public List<ChatRoomResponse> getMyChatRooms(Long myUserId) {
        log.info("Fetching chat rooms for user: {}", myUserId);

        // 1. 내 활성 매칭 조회
        List<Match> myMatches = matchRepository.findActiveMatchesByUserId(myUserId);
        
        if (myMatches.isEmpty()) {
            log.info("No active matches found for user: {}", myUserId);
            return List.of();
        }

        log.info("Found {} active matches for user: {}", myMatches.size(), myUserId);

        // 2. 채팅방 ID 목록 추출 (null 제외)
        List<String> chatRoomIds = myMatches.stream()
                .map(Match::getChatRoomId)
                .filter(id -> id != null && !id.isEmpty())
                .collect(Collectors.toList());

        // 3. 채팅 프로젝트에서 마지막 메시지 정보 조회
        Map<String, ChatServiceClient.ChatRoomInfo> chatRoomInfoMap = 
                chatServiceClient.getBatchChatRoomInfo(chatRoomIds);

        // 4. 상대방 유저 ID 목록 추출
        List<Long> otherUserIds = myMatches.stream()
                .map(match -> getOtherUserId(match, myUserId))
                .distinct()
                .collect(Collectors.toList());

        // 5. 상대방 프로필 정보 조회
        Map<Long, UserInfoEntity> userInfoMap = userRepository.findAllById(otherUserIds)
                .stream()
                .filter(user -> user.getUserInfo() != null)
                .collect(Collectors.toMap(
                        user -> user.getId(),
                        user -> user.getUserInfo()
                ));

        // 6. 매칭 정보 + 채팅방 정보 + 프로필 정보 조합
        List<ChatRoomResponse> chatRooms = myMatches.stream()
                .map(match -> buildChatRoomResponse(match, myUserId, chatRoomInfoMap, userInfoMap))
                .filter(response -> response != null)  // 정보가 없는 채팅방 제외
                .sorted((r1, r2) -> {
                    // 마지막 메시지 시간 기준 내림차순 정렬
                    if (r1.getLastMessageAt() == null) return 1;
                    if (r2.getLastMessageAt() == null) return -1;
                    return r2.getLastMessageAt().compareTo(r1.getLastMessageAt());
                })
                .collect(Collectors.toList());

        log.info("Returning {} chat rooms for user: {}", chatRooms.size(), myUserId);
        return chatRooms;
    }

    /**
     * 매칭에서 상대방 유저 ID 가져오기
     */
    private Long getOtherUserId(Match match, Long myUserId) {
        return match.getUser1Id().equals(myUserId) 
                ? match.getUser2Id() 
                : match.getUser1Id();
    }

    /**
     * ChatRoomResponse 생성
     */
    private ChatRoomResponse buildChatRoomResponse(
            Match match,
            Long myUserId,
            Map<String, ChatServiceClient.ChatRoomInfo> chatRoomInfoMap,
            Map<Long, UserInfoEntity> userInfoMap
    ) {
        Long otherUserId = getOtherUserId(match, myUserId);
        UserInfoEntity otherUserInfo = userInfoMap.get(otherUserId);

        // 상대방 프로필 정보가 없으면 스킵
        if (otherUserInfo == null) {
            log.warn("User info not found for user: {}", otherUserId);
            return null;
        }

        // 채팅방 정보 가져오기
        ChatServiceClient.ChatRoomInfo chatInfo = chatRoomInfoMap.get(match.getChatRoomId());

        // 상대방 정보 구성
        ChatRoomResponse.OtherUserInfo otherUser = ChatRoomResponse.OtherUserInfo.builder()
                .userId(otherUserId)
                .name(otherUserInfo.getName())
                .age(otherUserInfo.getAge())
                .profilePhoto(otherUserInfo.getPhotos() != null && !otherUserInfo.getPhotos().isEmpty() 
                        ? otherUserInfo.getPhotos().get(0) 
                        : null)
                .build();

        // 채팅방 응답 구성
        return ChatRoomResponse.builder()
                .chatRoomId(match.getChatRoomId())
                .matchId(match.getId())
                .matchType(match.getMatchType())
                .matchedAt(match.getMatchedAt())
                .otherUser(otherUser)
                .lastMessage(chatInfo != null ? chatInfo.getLastMessage() : null)
                .lastMessageAt(chatInfo != null && chatInfo.getLastMessageAt() != null 
                        ? Instant.parse(chatInfo.getLastMessageAt()) 
                        : null)
                .unreadCount(chatInfo != null ? chatInfo.getUnreadCount() : 0)
                .build();
    }
}
