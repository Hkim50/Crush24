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

    @Value("${apns.key.path:}")
    private String keyPath;

    @Value("${apns.team.id:}")
    private String teamId;

    @Value("${apns.key.id:}")
    private String keyId;

    @Value("${apns.production:false}")
    private boolean production;

    @Bean
    @ConditionalOnProperty(prefix = "apns", name = "enabled", havingValue = "true")
    public ApnsClient apnsClient() {
        try {
            // .p8 파일에서 signing key 로드
            ApnsSigningKey signingKey = ApnsSigningKey.loadFromPkcs8File(
                new File(keyPath),
                teamId,
                keyId
            );

            // APNs 클라이언트 빌드
            ApnsClient client = new ApnsClientBuilder()
                .setApnsServer(production 
                    ? ApnsClientBuilder.PRODUCTION_APNS_HOST 
                    : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                .setSigningKey(signingKey)
                .build();

            log.info("APNs client initialized successfully ({})", 
                     production ? "PRODUCTION" : "DEVELOPMENT");
            
            return client;

        } catch (IOException | NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to initialize APNs client", e);
            throw new RuntimeException("Failed to initialize APNs client", e);
        }
    }
}
