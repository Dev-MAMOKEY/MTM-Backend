package com.likelion.mtm.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Slf4j
@Component
public class JwtProvider {

    private static final String CLAIM_TYPE = "type";
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    private final SecretKey secretKey;

    private final long accessTokenExpMin;

    private final long refreshTokenExpDay;

    public JwtProvider(
            @Value("${custom.jwt.secret-key}") String secretKey,
            @Value("${custom.jwt.access-exp-min}") long accessTokenExpMin,
            @Value("${custom.jwt.refresh-exp-day}") long refreshTokenExpDay
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpMin = accessTokenExpMin * 60 * 1000;
        this.refreshTokenExpDay = refreshTokenExpDay * 24 * 60 * 60 * 1000;
    }

    public String createAccessToken(Long memberId) {
        return createToken(memberId, TYPE_ACCESS, accessTokenExpMin);
    }

    public String createRefreshToken(Long memberId) {
        return createToken(memberId, TYPE_REFRESH, refreshTokenExpDay);
    }

    public String createToken(Long memberId, String type, long expMillis) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(memberId))
                .claim(CLAIM_TYPE, type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expMillis))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getMemberId(String token) {
        return Long.parseLong(parseClaims(token).getSubject());
    }

    // 액세스 토큰으로 발급된 것이 맞는지 확인 (인증 필터에서 사용)
    // 리프레시 토큰을 Authorization 헤더로 보내 보호된 API를 호출하는 것을 막는다
    public boolean isAccessToken(String token) {
        return TYPE_ACCESS.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    // 리프레시 토큰으로 발급된 것이 맞는지 확인 (재발급에서만 사용)
    public boolean isRefreshToken(String token) {
        return TYPE_REFRESH.equals(parseClaims(token).get(CLAIM_TYPE, String.class));
    }

    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.debug("만료된 토큰입니다. : {}", e.getMessage());
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("유효하지 않은 토큰입니다. : {}", e.getMessage());
        }
        return false;
    }
}