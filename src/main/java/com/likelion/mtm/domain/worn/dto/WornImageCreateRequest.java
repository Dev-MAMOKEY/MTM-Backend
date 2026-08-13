package com.likelion.mtm.domain.worn.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 기준 이미지에 착용할 제품을 지정하는 착용 이미지 생성 요청 DTO.
 */
public record WornImageCreateRequest(
        @NotNull(message = "제품 식별자는 필수입니다.")
        Long productId
) {
}
