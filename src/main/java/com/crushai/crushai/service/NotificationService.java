package com.crushai.crushai.service;

import com.crushai.crushai.entity.UserEntity;
import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.PushNotificationResponse;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.TokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Service
@Slf4j
public class NotificationService {

    private final Optional<ApnsClient> apnsClient;
    
    @Value("${apns.topic}")
    private String apnsTopic; // iOS 앱의 Bundle ID
    
    // ApnsClient가 없을 수 있음 (개발 초기에는 설정 안됨)
    @Autowired
    public NotificationService(Optional<ApnsClient> apnsClient) {
        this.apnsClient = apnsClient;
        if (apnsClient.isEmpty()) {
            log.warn("ApnsClient is not configured. Push notifications will not be sent.");
        } else {
            log.info("ApnsClient is configured. Push notifications are enabled.");
        }
    }
    
    /**
     * 좋아요 알림 전송
     */
    public void sendLikeNotification(UserEntity toUser, UserEntity fromUser) {
        log.info("Sending like notification to user: {} from user: {}", 
                 toUser.getId(), fromUser.getId());
        
        if (apnsClient.isEmpty()) {
            log.warn("APNs not configured. Notification not sent.");
            return;
        }
        
        String deviceToken = toUser.getApnsToken();
        if (deviceToken == null || deviceToken.isEmpty()) {
            log.warn("User {} has no APNs token", toUser.getId());
            return;
        }
        
        String fromNickname = fromUser.getUserInfo() != null 
            ? fromUser.getUserInfo().getNickname() 
            : "Someone";
        
        String title = "New Like! 💕";
        String body = fromNickname + " likes you!";
        
        sendPushNotification(deviceToken, title, body, "like", fromUser.getId().toString());
    }
    
    /**
     * 매칭 알림 전송
     */
    public void sendMatchNotification(UserEntity user, UserEntity matchedUser) {
        log.info("Sending match notification to user: {} about match with: {}", 
                 user.getId(), matchedUser.getId());
        
        if (apnsClient.isEmpty()) {
            log.warn("APNs not configured. Notification not sent.");
            return;
        }
        
        String deviceToken = user.getApnsToken();
        if (deviceToken == null || deviceToken.isEmpty()) {
            log.warn("User {} has no APNs token", user.getId());
            return;
        }
        
        String matchedNickname = matchedUser.getUserInfo() != null 
            ? matchedUser.getUserInfo().getNickname() 
            : "Someone";
        
        String title = "It's a Match! 🎉";
        String body = "You and " + matchedNickname + " liked each other!";
        
        sendPushNotification(deviceToken, title, body, "match", matchedUser.getId().toString());
    }
    
    /**
     * APNs 푸시 알림 전송
     */
    private void sendPushNotification(String deviceToken, String title, String body, 
                                     String notificationType, String userId) {
        if (apnsClient.isEmpty()) {
            log.warn("APNs client not configured. Notification not sent.");
            return;
        }
        
        try {
            // 디바이스 토큰 정리 (공백, < > 제거)
            String sanitizedToken = TokenUtil.sanitizeTokenString(deviceToken);
            
            // 페이로드 빌드
            SimpleApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
            payloadBuilder.setAlertTitle(title);
            payloadBuilder.setAlertBody(body);
            payloadBuilder.setSound("default");
            payloadBuilder.setBadgeNumber(1); // TODO: 실제 읽지 않은 알림 수로 교체
            
            // Custom data 추가
            payloadBuilder.addCustomProperty("type", notificationType);
            payloadBuilder.addCustomProperty("userId", userId);
            
            String payload = payloadBuilder.build();
            
            // 푸시 알림 생성
            SimpleApnsPushNotification pushNotification = new SimpleApnsPushNotification(
                sanitizedToken,
                apnsTopic,
                payload
            );
            
            // 비동기로 전송
            apnsClient.get().sendNotification(pushNotification).whenComplete((response, cause) -> {
                if (response != null) {
                    if (response.isAccepted()) {
                        log.info("Push notification accepted by APNs gateway for token: {}", 
                                sanitizedToken.substring(0, 8) + "...");
                    } else {
                        log.error("Notification rejected by APNs gateway: {}", 
                                 response.getRejectionReason());
                        
                        response.getTokenInvalidationTimestamp().ifPresent(timestamp -> {
                            log.error("Token is invalid as of {}", timestamp);
                            // TODO: 토큰 무효화 처리 (DB에서 제거)
                        });
                    }
                } else {
                    log.error("Failed to send push notification", cause);
                }
            });
            
        } catch (Exception e) {
            log.error("Error sending push notification", e);
        }
    }
    
    /**
     * 채팅 메시지 알림 (나중에 채팅 서버에서 호출 가능)
     */
    public void sendChatMessageNotification(UserEntity toUser, UserEntity fromUser, String message) {
        log.info("Sending chat message notification to user: {} from user: {}", 
                 toUser.getId(), fromUser.getId());
        
        if (apnsClient.isEmpty()) {
            log.warn("APNs not configured. Notification not sent.");
            return;
        }
        
        String deviceToken = toUser.getApnsToken();
        if (deviceToken == null || deviceToken.isEmpty()) {
            log.warn("User {} has no APNs token", toUser.getId());
            return;
        }
        
        String fromNickname = fromUser.getUserInfo() != null 
            ? fromUser.getUserInfo().getNickname() 
            : "Someone";
        
        String title = fromNickname;
        String body = message.length() > 100 ? message.substring(0, 100) + "..." : message;
        
        sendPushNotification(deviceToken, title, body, "chat", fromUser.getId().toString());
    }
}
