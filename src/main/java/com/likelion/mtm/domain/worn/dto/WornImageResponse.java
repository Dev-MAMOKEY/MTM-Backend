package com.likelion.mtm.domain.worn.dto;

import com.likelion.mtm.domain.worn.entity.Generator;
import com.likelion.mtm.domain.worn.entity.WornImage;

import java.time.LocalDateTime;

/**
 * 생성된 착용 이미지 정보를 반환하는 응답 DTO.
 */
public record WornImageResponse(
        Long id,
        Long baseImageId,
        Long productId,
        Long productCutId,
        String imageUrl,
        Generator generator,
        LocalDateTime createdAt
) {

    /**
     * 착용 이미지 엔티티와 이미지 접근 URL을 응답 DTO로 변환한다.
     */
    public static WornImageResponse from(WornImage wornImage, String imageUrl) {
        return new WornImageResponse(
                wornImage.getId(),
                wornImage.getBaseImage().getId(),
                wornImage.getProduct().getId(),
                wornImage.getProductCut() == null ? null : wornImage.getProductCut().getId(),
                imageUrl,
                wornImage.getGenerator(),
                wornImage.getCreatedAt()
        );
    }
}
