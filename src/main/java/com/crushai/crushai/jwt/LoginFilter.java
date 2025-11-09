package com.crushai.crushai.jwt;

import com.crushai.crushai.dto.CustomUserDetails;
import com.crushai.crushai.dto.JoinDto;
import com.crushai.crushai.entity.RefreshEntity;
import com.crushai.crushai.repository.RefreshRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

public class LoginFilter extends UsernamePasswordAuthenticationFilter {

    private final AuthenticationManager authenticationManager;
    private final JWTUtil jwtUtil;
    private final RefreshRepository repository;

    public LoginFilter(AuthenticationManager authenticationManager, JWTUtil jwtUtil, RefreshRepository repository) {
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.repository = repository;
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            JoinDto joinDto = objectMapper.readValue(request.getInputStream(), JoinDto.class);

            String email = joinDto.getEmail();
            String password = joinDto.getPassword();

            if (password == null || password.isEmpty()) {
                throw new BadCredentialsException("This account is a social login user.");
            }

            UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(email, password, null);

            return authenticationManager.authenticate(authToken);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    @Override
    protected String obtainUsername(HttpServletRequest request) {
        return request.getParameter("email");
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authentication) throws IOException, ServletException {
        // CustomUserDetails에서 userId 추출
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long userId = userDetails.getUserId();
        String email = userDetails.getUsername();

        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        Iterator<? extends GrantedAuthority> iterator = authorities.iterator();
        GrantedAuthority auth = iterator.next();
        String role = auth.getAuthority();

        //토큰 생성 (userId 포함)
        String access = jwtUtil.createJwt("access", email, role, userId, 600000L);
        String refresh = jwtUtil.createJwt("refresh", email , role, userId, 86400000L);

        //응답 설정
        response.setHeader("access", access);
        response.setHeader("refresh", refresh);

        //Refresh 토큰 저장
        addRefreshEntity(email, refresh, 86400000L);

        String json = String.format("{\"accessToken\": \"%s\", \"refreshToken\": \"%s\"}", access, refresh);

        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");
        response.setStatus(HttpStatus.OK.value());
        response.getWriter().write(json);
    }

    private void addRefreshEntity(String username, String refresh, Long expiredMs) {

        Date date = new Date(System.currentTimeMillis() + expiredMs);

        RefreshEntity refreshEntity = new RefreshEntity(username, refresh, date.toString());

        repository.save(refreshEntity);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("utf-8");

        String json = String.format("{\"error\": \"Authentication failed\", \"message\": \"%s\"}", failed.getMessage());

        response.getWriter().write(json);

    }


}
