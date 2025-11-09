package com.crushai.crushai.jwt;

import com.crushai.crushai.repository.RefreshRepository;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.GenericFilterBean;

import java.io.IOException;
import java.io.PrintWriter;

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

        String refresh = request.getHeader("refreshToken");

        if (refresh == null || refresh.trim().isEmpty()) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Refresh token is missing.");
            return;
        }

        String category = jwtUtil.getCategory(refresh);
        if (!category.equals("refreshToken")) {
            sendJsonError(response, HttpServletResponse.SC_BAD_REQUEST, "Invalid token category.");
            return;
        }

        // delete all refreshToken by user's email
        // 유저 로그아웃 시 모든기기에서 로그아웃
        String userEmail = jwtUtil.getUsername(refresh);
        refreshRepository.deleteAllByEmail(userEmail);

        // Success response
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\": \"Logout successful\"}");
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
}
