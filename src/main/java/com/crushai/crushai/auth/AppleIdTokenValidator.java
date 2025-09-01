package com.crushai.crushai.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;

@Component
public class AppleIdTokenValidator {

    private static final String APPLE_KEYS_URL = "https://appleid.apple.com/auth/keys";

    /**
     * Apple Identity Token 검증
     *
     * @param identityToken iOS에서 전달받은 Identity Token
     * @param clientId      iOS 앱의 서비스 ID (App Bundle ID)
     * @return claims 정보, 검증 실패 시 null
     */
    public Map<String, Object> verify(String identityToken, String clientId) {
        try {
            SignedJWT signedJWT = SignedJWT.parse(identityToken);

            // Apple 공개 키 가져오기
            JWKSet jwkSet = JWKSet.load(new URL(APPLE_KEYS_URL));
            JWK jwk = jwkSet.getKeyByKeyId(signedJWT.getHeader().getKeyID());
            if (jwk == null) return null;

            // RSAPublicKey 변환 후 verifier 생성
            RSAPublicKey publicKey = jwk.toRSAKey().toRSAPublicKey();
            JWSVerifier verifier = new RSASSAVerifier(publicKey);

            // 서명 검증
            if (!signedJWT.verify(verifier)) return null;

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // 토큰 만료 확인
            Date now = new Date();
            if (claims.getExpirationTime() == null || claims.getExpirationTime().before(now)) return null;

            // audience 검증 (앱 번들 ID와 일치해야 함)
            if (!claims.getAudience().contains(clientId)) return null;

            // issuer 검증
            if (!"https://appleid.apple.com".equals(claims.getIssuer())) return null;

            return claims.getClaims();
        } catch (ParseException | JOSEException | java.io.IOException e) {
            e.printStackTrace();
        }

        return null;
    }
}
