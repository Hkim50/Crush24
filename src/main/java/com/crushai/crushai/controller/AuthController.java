package com.crushai.crushai.controller;

import com.crushai.crushai.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@RequestBody Map<String, String> body) {
        String idToken = body.get("idToken");
        String deviceId = body.get("deviceId");
        String deviceName = body.get("deviceName");

        System.out.println("Google login Request: " + body.toString());

        Map<String, String> tokens = authService.loginWithGoogle(idToken, deviceId, deviceName);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/apple")
    public ResponseEntity<?> appleLogin(@RequestBody Map<String, String> body) {
        String identityToken = body.get("identityToken");
        String clientId = body.get("clientId");
        String deviceId = body.get("deviceId");
        String deviceName = body.get("deviceName");

        System.out.println("Apple login Request: " + body.toString());

        Map<String, String> tokens = authService.loginWithApple(identityToken, clientId, deviceId, deviceName);
        return ResponseEntity.ok(tokens);
    }

    @PostMapping("/facebook")
    public ResponseEntity<?> facebookLogin(@RequestBody Map<String, String> body) {
        String accessToken = body.get("accessToken");
        String deviceId = body.get("deviceId");
        String deviceName = body.get("deviceName");

        System.out.println("Facebook login");

        System.out.println(accessToken);

        Map<String, String> tokens = authService.loginWithFacebook(accessToken, deviceId, deviceName);
        return ResponseEntity.ok(tokens);
    }
}

