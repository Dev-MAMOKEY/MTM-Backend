package com.likelion.mtm.domain.member.dto;

import com.likelion.mtm.domain.member.entity.Member;

import java.math.BigDecimal;

public record MemberResponseDTO(
        Long id,
        String email,
        BigDecimal heightCm,
        BigDecimal weightKg
) {
    public static MemberResponseDTO from(Member member) {
        return new MemberResponseDTO(
                member.getId(),
                member.getEmail(),
                member.getHeightCm(),
                member.getWeightKg()
        );
    }
}