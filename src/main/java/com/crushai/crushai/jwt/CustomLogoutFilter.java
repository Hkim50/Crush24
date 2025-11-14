package com.crushai.crushai.jwt;

import com.crushai.crushai.repository.RefreshRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
public class CustomLogoutFilter extends GenericFilterBean {

    private final JWTUtil jwtUtil;
    private final RefreshRepository refreshRepository;

    public CustomLogoutFilter(JWTUtil jwtUtil, RefreshRepository refreshRepository) {
        this.jwtUtil = jwtUtil;
        this.refreshRepository = refreshRepository;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        doFilter((HttpServletRequest) request, (HttpServletResponse) response, chain);
    }

    private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws IOException, ServletException {

        if (!request.getRequestURI().equals("/logout") || !request.getMethod().equals("POST")) {
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = request.getHeader("refreshToken");
        String logoutType = request.getHeader("logoutType"); // "current" 또는 "all"

        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Refresh token is missing.");
            return;
        }

        String category = jwtUtil.getCategory(refreshToken);
        if (!category.equals("refreshToken")) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid token category.");
            return;
        }

        Long userId = jwtUtil.getUserId(refreshToken);
        String tokenHash = hashToken(refreshToken);
        
        // logoutType에 따라 처리
        if ("all".equals(logoutType)) {
            // 모든 디바이스 로그아웃
            refreshRepository.deleteAllByUser_Id(userId);
            log.info("All devices logged out for user: {}", userId);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"Logged out from all devices\"}");
        } else {
            // 현재 디바이스만 로그아웃 (기본)
            refreshRepository.deleteByTokenHash(tokenHash);
            log.info("Current device logged out for user: {}", userId);
            
            response.setStatus(HttpServletResponse.SC_OK);
            response.setContentType("application/json");
            response.getWriter().write("{\"message\": \"Logout successful\"}");
        }
    }

    private void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");

        String json = String.format("{\"error\": \"%s\"}", message);
        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }
    
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (Exception e) {
            throw new RuntimeException("Failed to hash token", e);
        }
    }
}
