package com.likelion.mtm.domain.member.controller;

import com.likelion.mtm.domain.member.dto.MemberResponseDTO;
import com.likelion.mtm.domain.member.service.MemberService;
import com.likelion.mtm.global.rsdata.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 회원 API — 로그인한 회원이 자기 정보를 다루는 엔드포인트 */
@Tag(name = "회원", description = "내 정보 조회 · 신체 정보")
@RestController
@RequestMapping("/api/v1/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "내 정보 조회", description = "액세스 토큰으로 식별된 회원의 정보를 돌려준다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 만료됨")
    })
    @GetMapping("/me")
    public ResponseEntity<RsData<MemberResponseDTO>> getMyInfo(@AuthenticationPrincipal Long memberId) {
        // memberId는 JwtAuthenticationFilter가 SecurityContext에 넣어 둔 값이다
        return ResponseEntity.ok(RsData.success(memberService.getMyInfo(memberId)));
    }
}