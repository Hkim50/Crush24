package com.crushai.crushai.config;

import com.eatthepath.pushy.apns.ApnsClient;
import com.eatthepath.pushy.apns.ApnsClientBuilder;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Configuration
@Slf4j
public class ApnsConfig {

    @Value("${apns.key.path}")
    private String keyPath;

    @Value("${apns.team.id}")
    private String teamId;

    @Value("${apns.key.id}")
    private String keyId;

    @Value("${apns.production:false}")
    private boolean production;

    /**
     * APNs 클라이언트 빈 생성
     * apns.enabled=true일 때만 활성화
     * 
     * Token-based authentication 사용 (공식 권장 방식)
     * - .p8 파일을 사용한 JWT 토큰 인증
     * - 여러 앱에 동일한 키 사용 가능
     * - TLS 인증서보다 간편하고 유연함
     */
    @Bean
    @ConditionalOnProperty(prefix = "apns", name = "enabled", havingValue = "true")
    public ApnsClient apnsClient() {
        try {
            log.info("Initializing APNs client...");
            log.info("Key path: {}", keyPath);
            log.info("Team ID: {}", teamId);
            log.info("Key ID: {}", keyId);
            log.info("Production mode: {}", production);
            
            // .p8 파일에서 signing key 로드 (Pushy 공식 방식)
            ApnsSigningKey signingKey = ApnsSigningKey.loadFromPkcs8File(
                new File(keyPath),
                teamId,
                keyId
            );

            // APNs 클라이언트 빌드
            ApnsClient client = new ApnsClientBuilder()
                .setApnsServer(production 
                    ? ApnsClientBuilder.PRODUCTION_APNS_HOST   // api.push.apple.com
                    : ApnsClientBuilder.DEVELOPMENT_APNS_HOST) // api.sandbox.push.apple.com
                .setSigningKey(signingKey)
                .build();

            log.info("✅ APNs client initialized successfully");
            log.info("Server: {}", production ? "PRODUCTION" : "DEVELOPMENT");
            
            return client;

        } catch (IOException e) {
            log.error("❌ Failed to load APNs signing key from: {}", keyPath, e);
            log.error("Please check if the .p8 file exists and is readable");
            throw new RuntimeException("Failed to load APNs signing key", e);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("❌ Invalid APNs signing key format", e);
            log.error("Please ensure you're using a valid .p8 file from Apple Developer");
            throw new RuntimeException("Invalid APNs signing key", e);
        } catch (Exception e) {
            log.error("❌ Unexpected error initializing APNs client", e);
            throw new RuntimeException("Failed to initialize APNs client", e);
        }
    }
}
