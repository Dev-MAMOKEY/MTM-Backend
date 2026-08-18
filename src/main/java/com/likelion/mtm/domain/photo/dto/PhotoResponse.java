package com.likelion.mtm.domain.photo.dto;

import com.likelion.mtm.domain.photo.entity.Photo;

import java.time.LocalDateTime;

/**
 * 사진첩에서 원본 사진 정보를 반환하기 위한 응답 DTO.
 * baseImage는 아직 기준 이미지를 만들지 않은 사진이면 null이다.
 */
public record PhotoResponse(
        Long id,
        String imageUrl,
        LocalDateTime createdAt,
        BaseImageResponse baseImage
) {

    /**
     * 원본 사진 엔티티와 이미지 접근 URL, 기준 이미지를 응답 DTO로 변환한다.
     *
     * @param photo 원본 사진 엔티티
     * @param imageUrl 이미지 접근 URL
     * @param baseImage 사진에 연결된 기준 이미지. 아직 없으면 null
     * @return 원본 사진 응답 DTO
     */
    public static PhotoResponse from(Photo photo, String imageUrl, BaseImageResponse baseImage) {
        return new PhotoResponse(
                photo.getId(),
                imageUrl,
                photo.getCreatedAt(),
                baseImage
        );
    }
}