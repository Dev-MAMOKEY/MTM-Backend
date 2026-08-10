package com.likelion.mtm.domain.member.service;

import com.likelion.mtm.domain.member.dto.LoginRequestDTO;
import com.likelion.mtm.domain.member.dto.SignupRequestDTO;
import com.likelion.mtm.domain.member.dto.TokenReissueRequestDTO;
import com.likelion.mtm.domain.member.dto.TokenResponseDTO;
import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.member.repository.MemberRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.likelion.mtm.global.security.JwtProvider;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberAuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    @Transactional
    public void signup(SignupRequestDTO request) {

        if (memberRepository.existsByEmail(request.email())) {
            throw new CustomException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        Member member = Member.register(
                request.email(),
                passwordEncoder.encode(request.password())
        );
        memberRepository.save(member);
    }

    public TokenResponseDTO login(LoginRequestDTO request) {

        Member member = memberRepository.findByEmail(request.email())
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_CREDENTIALS));

        if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
            throw new CustomException(ErrorCode.INVALID_CREDENTIALS);
        }

        return new TokenResponseDTO(
                jwtProvider.createAccessToken(member.getId()),
                jwtProvider.createRefreshToken(member.getId())
        );
    }

    public TokenResponseDTO reissue(TokenReissueRequestDTO request) {
        String refreshToken = request.refreshToken();

        Long memberId;
        try {
            // 리프레시 토큰으로 발급된 것이 맞는지 확인 — 액세스 토큰을 넣어 무한 갱신하는 걸 막는다
            if (!jwtProvider.isRefreshToken(refreshToken)) {
                throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
            }
            memberId = jwtProvider.getMemberId(refreshToken);
        } catch (ExpiredJwtException e) {
            // 만료는 재로그인 신호다. 프론트가 무효(위조)와 구분해서 처리할 수 있게 코드를 나눈다
            throw new CustomException(ErrorCode.REFRESH_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            throw new CustomException(ErrorCode.REFRESH_TOKEN_INVALID);
        }

        // 탈퇴·삭제된 회원의 토큰으로 재발급되는 것을 막는다
        if (!memberRepository.existsById(memberId)) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }

        return new TokenResponseDTO(jwtProvider.createAccessToken(memberId), refreshToken);
    }
}