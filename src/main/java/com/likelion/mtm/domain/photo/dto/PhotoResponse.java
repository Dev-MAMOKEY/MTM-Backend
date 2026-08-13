package com.likelion.mtm.domain.photo.dto;

import com.likelion.mtm.domain.photo.entity.Photo;

import java.time.LocalDateTime;

/**
 * 사진첩에서 원본 사진 정보를 반환하기 위한 응답 DTO.
 */
public record PhotoResponse(
        Long id,
        String imageUrl,
        LocalDateTime createdAt
) {

    /**
     * 원본 사진 엔티티와 이미지 접근 URL을 응답 DTO로 변환한다.
     *
     * @param photo 원본 사진 엔티티
     * @param imageUrl 이미지 접근 URL
     * @return 원본 사진 응답 DTO
     */
    public static PhotoResponse from(Photo photo, String imageUrl) {
        return new PhotoResponse(
                photo.getId(),
                imageUrl,
                photo.getCreatedAt()
        );
    }
}