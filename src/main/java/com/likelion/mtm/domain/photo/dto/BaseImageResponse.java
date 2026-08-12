package com.likelion.mtm.domain.photo.dto;

import com.likelion.mtm.domain.photo.entity.BaseImage;

import java.time.LocalDateTime;

/**
 * 생성되거나 재사용된 기준 이미지 정보를 반환하는 응답 DTO.
 */
public record BaseImageResponse(
        Long id,
        Long photoId,
        String imageUrl,
        LocalDateTime createdAt
) {

    /**
     * 기준 이미지 엔티티와 접근 URL을 응답 DTO로 변환한다.
     */
    public static BaseImageResponse from(BaseImage baseImage, String imageUrl) {
        return new BaseImageResponse(
                baseImage.getId(),
                baseImage.getPhoto().getId(),
                imageUrl,
                baseImage.getCreatedAt()
        );
    }
}
