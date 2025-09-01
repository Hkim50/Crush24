package com.crushai.crushai.controller;

import com.crushai.crushai.entity.Role;
import com.crushai.crushai.jwt.JWTUtil;
import com.crushai.crushai.service.TokenService;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReissueController {

    private TokenService tokenService;

    public ReissueController(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @PostMapping("/reissue")
    public ResponseEntity<?> reissue(HttpServletRequest request) {
        String token = request.getHeader("refresh");
        return tokenService.reissueTokens(token);
    }
}
