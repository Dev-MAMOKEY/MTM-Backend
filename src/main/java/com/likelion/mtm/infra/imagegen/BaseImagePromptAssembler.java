package com.likelion.mtm.infra.imagegen;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * 원본 사진을 기준 이미지로 변환하기 위한 프롬프트를 조립한다.
 */
@Component
public class BaseImagePromptAssembler {

    /**
     * 회원의 신체 정보를 포함한 기준 이미지 생성 프롬프트를 만든다.
     *
     * @param heightCm 회원 키(cm)
     * @param weightKg 회원 몸무게(kg)
     * @return 기준 이미지 생성 프롬프트
     */
    public String assemble(BigDecimal heightCm, BigDecimal weightKg) {
        Objects.requireNonNull(heightCm, "회원 키는 필수입니다.");
        Objects.requireNonNull(weightKg, "회원 몸무게는 필수입니다.");

        return """
                Transform the provided full-body photo into a photorealistic reference image.
                Preserve the same person's identity, face, body characteristics, hairstyle, and clothing.
                The person's height is %s cm and weight is %s kg; reflect these body proportions naturally.
                Show exactly one person standing upright in a neutral, symmetrical, front-facing posture.
                Keep the entire body visible from head to feet and centered in the frame.
                Use an evenly lit, solid pure white background with no objects, shadows, or scenery.
                Keep the background opaque. Do not remove the background and do not create transparency.
                Do not add accessories, products, text, or watermarks.
                """.formatted(heightCm.toPlainString(), weightKg.toPlainString());
    }
}
