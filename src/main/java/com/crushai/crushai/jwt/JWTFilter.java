package com.crushai.crushai.jwt;


import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.entity.Role;
import com.crushai.crushai.entity.UserEntity;
import io.jsonwebtoken.ExpiredJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;

    public JWTFilter(JWTUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String accessToken = request.getHeader("accessToken");

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtUtil.isExpired(accessToken);
        } catch (ExpiredJwtException e) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Access token expired");
            return;
        }

        String category = jwtUtil.getCategory(accessToken);

        if (!category.equals("accessToken")) {
            sendJsonError(response, HttpServletResponse.SC_UNAUTHORIZED, "Invalid access token");
            return;
        }

        String email = jwtUtil.getUsername(accessToken);
        String role = jwtUtil.getRole(accessToken);

        if (role.startsWith("ROLE_")) {
            role = role.substring(5);
        }

        Role userRole = Role.valueOf(role);
        UserEntity userEntity = new UserEntity(email, userRole);
        CustomUserDetails customUserDetails = new CustomUserDetails(userEntity);

        Authentication authToken = new UsernamePasswordAuthenticationToken(
                customUserDetails, null, customUserDetails.getAuthorities());

        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }


    private void sendJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");

        String json = String.format("{\"error\": \"%s\"}", message);
        response.getWriter().write(json);
    }

}
