package com.likelion.mtm.domain.member.controller;

import com.likelion.mtm.domain.member.dto.LoginRequestDTO;
import com.likelion.mtm.domain.member.dto.SignupRequestDTO;
import com.likelion.mtm.domain.member.dto.TokenReissueRequestDTO;
import com.likelion.mtm.domain.member.dto.TokenResponseDTO;
import com.likelion.mtm.domain.member.service.MemberAuthService;
import com.likelion.mtm.global.rsdata.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 인증 API — 토큰이 없는 상태에서 호출하는 엔드포인트만 모은다 */
@Tag(name = "인증", description = "회원가입 · 로그인 · 토큰 재발급")
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@SecurityRequirements // 이 그룹은 토큰이 필요 없다 — Swagger에서 자물쇠 표시를 뗀다
public class AuthController {

    private final MemberAuthService memberAuthService;

    @Operation(summary = "회원가입", description = "이메일과 비밀번호로 가입한다. 비밀번호는 해시로 저장된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "가입 성공"),
            @ApiResponse(responseCode = "400", description = "이메일 형식 오류 또는 비밀번호 길이 위반"),
            @ApiResponse(responseCode = "409", description = "이미 가입된 이메일")
    })
    @PostMapping("/signup")
    public ResponseEntity<RsData<String>> signup(@Valid @RequestBody SignupRequestDTO request) {
        memberAuthService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RsData.success("회원가입이 완료되었습니다."));
    }

    @Operation(summary = "로그인", description = "액세스 토큰과 리프레시 토큰을 발급한다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공"),
            @ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 불일치")
    })
    @PostMapping("/login")
    public ResponseEntity<RsData<TokenResponseDTO>> login(@Valid @RequestBody LoginRequestDTO request) {
        return ResponseEntity.ok(RsData.success(memberAuthService.login(request)));
    }

    @Operation(summary = "액세스 토큰 재발급",
            description = "리프레시 토큰으로 새 액세스 토큰을 받는다. 리프레시 토큰은 그대로 유지된다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재발급 성공"),
            @ApiResponse(responseCode = "401", description = "리프레시 토큰이 만료되었거나 유효하지 않음 — 재로그인 필요")
    })
    @PostMapping("/reissue")
    public ResponseEntity<RsData<TokenResponseDTO>> reissue(@Valid @RequestBody TokenReissueRequestDTO request) {
        return ResponseEntity.ok(RsData.success(memberAuthService.reissue(request)));
    }
}