package com.likelion.mtm.domain.photo.service;

import com.likelion.mtm.domain.photo.dto.BaseImageResponse;
import com.likelion.mtm.domain.photo.entity.BaseImage;
import com.likelion.mtm.domain.photo.entity.Photo;
import com.likelion.mtm.domain.photo.repository.BaseImageRepository;
import com.likelion.mtm.domain.photo.repository.PhotoRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.likelion.mtm.infra.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * 기준 이미지의 조회와 동시 생성 시 저장 확정을 짧은 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class BaseImagePersistenceService {

    private final PhotoRepository photoRepository;
    private final BaseImageRepository baseImageRepository;
    private final ImageStorage imageStorage;

    /**
     * 이미 만들어진 기준 이미지를 트랜잭션 안에서 응답 DTO로 변환한다.
     */
    @Transactional(readOnly = true)
    public Optional<BaseImageResponse> findExisting(Long photoId) {
        return baseImageRepository.findByPhotoId(photoId)
                .map(this::toResponse);
    }

    /**
     * 원본 사진 행을 잠근 뒤 기준 이미지가 없을 때만 새 저장소 키를 연결한다.
     */
    @Transactional
    public FinalizationResult finalizeCreation(Long photoId, String newStorageKey) {
        Photo photo = photoRepository.findByIdForUpdate(photoId)
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        Optional<BaseImage> existing = baseImageRepository.findByPhotoId(photoId);
        if (existing.isPresent()) {
            return new FinalizationResult(toResponse(existing.get()), false);
        }

        BaseImage savedBaseImage = baseImageRepository.save(
                BaseImage.create(photo, newStorageKey)
        );

        return new FinalizationResult(toResponse(savedBaseImage), true);
    }

    /**
     * 기준 이미지와 저장소 URL을 응답 DTO로 변환한다.
     */
    private BaseImageResponse toResponse(BaseImage baseImage) {
        return BaseImageResponse.from(
                baseImage,
                imageStorage.getUrl(baseImage.getStorageKey())
        );
    }

    /**
     * 저장 확정 결과와 새 이미지가 실제 DB에 연결됐는지를 함께 전달한다.
     */
    public record FinalizationResult(
            BaseImageResponse response,
            boolean created
    ) {
    }
}
