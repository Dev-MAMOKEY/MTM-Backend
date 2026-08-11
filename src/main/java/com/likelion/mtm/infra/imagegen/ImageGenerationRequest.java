package com.likelion.mtm.infra.imagegen;

import java.util.List;
import java.util.Objects;

/**
 * 공급자와 무관한 이미지 생성 요청.
 * 여러 입력 이미지를 받을 수 있어 기준 이미지와 이후 착용 이미지 생성에 함께 사용한다.
 */
public record ImageGenerationRequest(
        List<ImageInput> images,
        String prompt
) {

    public ImageGenerationRequest {
        Objects.requireNonNull(images, "입력 이미지 목록은 필수입니다.");
        Objects.requireNonNull(prompt, "프롬프트는 필수입니다.");

        if (images.isEmpty()) {
            throw new IllegalArgumentException("입력 이미지는 한 장 이상이어야 합니다.");
        }

        if (prompt.isBlank()) {
            throw new IllegalArgumentException("프롬프트는 비어 있을 수 없습니다.");
        }

        images = List.copyOf(images);
    }
}
