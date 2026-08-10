package com.likelion.mtm.domain.member.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

// 신체 정보 입력 요청
public record BodyInfoRequestDTO(
        @NotNull(message = "키는 필수입니다.")
        @DecimalMin(value = "100.0", message = "최소 키는 100.0cm 이상으로 입력해주세요.")
        @DecimalMax(value = "250.0", message = "최대 키는 250.0cm 이하로 입력해주세요.")
        @Digits(integer = 3, fraction = 1, message = "키는 소수점 첫째 자리까지 입력 가능합니다.")
        BigDecimal heightCm,

        @NotNull(message = "몸무게는 필수입니다.")
        @DecimalMin(value = "30.0", message = "최소 몸무게는 30.0kg 이상으로 입력해주세요.")
        @DecimalMax(value = "300.0", message = "최대 몸무게는 300.0kg 이하로 입력해주세요.")
        @Digits(integer = 3, fraction = 1, message = "몸무게는 소수점 첫째 자리까지 입력 가능합니다.")
        BigDecimal weightKg
) {
}