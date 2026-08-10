package com.likelion.mtm.global.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter { // 클라이언트 요청 1개당 정확히 1번만 실행됨

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String token = resolveToken(request); // 요청 헤더에서 토큰 꺼내기

        if (token != null && jwtProvider.validateToken(token) && jwtProvider.isAccessToken(token)) {
            Long memberId = jwtProvider.getMemberId(token);


            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(memberId, null, List.of());

            SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContext에 인증 정보 저장 -> 요청을 통해 인증된 사용자라고 스프링에 알리는 방법
        }

        filterChain.doFilter(request, response);
    }

    // Authorization 헤더에서 순수 토큰만 꺼내는 메서드
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith(BEARER_PREFIX)) {
            return bearerToken.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}