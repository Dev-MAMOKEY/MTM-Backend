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

        if (token != null && jwtProvider.validateToken(token)) {
            Long memberId = jwtProvider.getMemberId(token);

            // 회원 등급 구분이 없으므로 권한 목록은 비운다. principal에는 회원 식별자만 담는다
            // → 컨트롤러에서 @AuthenticationPrincipal Long memberId 로 바로 받을 수 있다
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(memberId, null, List.of());

            SecurityContextHolder.getContext().setAuthentication(authentication); // SecurityContext에 인증 정보 저장 -> 요청을 통해 인증된 사용자라고 스프링에 알리는 방법
        }
        // 토큰이 없거나 검증에 실패해도 여기서 끊지 않는다.
        // 인증 정보를 넣지 않은 채 통과시키고, 보호된 경로였다면 JwtAuthenticationEntryPoint가 401을 만든다
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