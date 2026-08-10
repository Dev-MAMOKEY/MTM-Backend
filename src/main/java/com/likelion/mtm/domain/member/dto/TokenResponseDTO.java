package com.likelion.mtm.domain.member.dto;

public record TokenResponseDTO(
        String accessToken,
        String refreshToken
) {
}