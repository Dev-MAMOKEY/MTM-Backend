package com.likelion.mtm.domain.member.dto;

import jakarta.validation.constraints.NotBlank;

/** 액세스 토큰 재발급 요청. 액세스 토큰이 만료된 상태에서 호출하므로 리프레시 토큰만 받는다 */
public record TokenReissueRequestDTO(
        @NotBlank(message = "리프레시 토큰은 필수입니다.")
        String refreshToken
) {
}