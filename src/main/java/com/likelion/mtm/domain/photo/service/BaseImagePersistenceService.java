package com.likelion.mtm.domain.photo.service;

import com.likelion.mtm.domain.photo.dto.BaseImageResponse;
import com.likelion.mtm.domain.photo.entity.BaseImage;
import com.likelion.mtm.domain.photo.entity.Photo;
import com.likelion.mtm.domain.photo.repository.BaseImageRepository;
import com.likelion.mtm.domain.photo.repository.PhotoRepository;
import com.likelion.mtm.domain.worn.entity.WornImage;
import com.likelion.mtm.domain.worn.repository.WornImageRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.likelion.mtm.infra.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * 기준 이미지의 조회와 동시 생성·재생성 시 저장 확정을 짧은 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class BaseImagePersistenceService {

    private final PhotoRepository photoRepository;
    private final BaseImageRepository baseImageRepository;
    private final WornImageRepository wornImageRepository;
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
     * 기준 이미지 행을 잠근 뒤 새 저장소 키로 교체하고, 그 기준 이미지에 딸린 착용 이미지를 모두
     * 함께 삭제한다 — 옛 기준 이미지 위의 착용 이미지는 새 기준 이미지와 얼굴이 다르기 때문이다.
     * 교체 전 저장소 키와 삭제된 착용 이미지들의 저장소 키를 함께 돌려줘 호출자가 트랜잭션 밖에서
     * 옛 파일들을 정리할 수 있게 한다.
     */
    @Transactional
    public RegenerationResult finalizeRegeneration(Long photoId, String newStorageKey) {
        BaseImage lockedBaseImage = baseImageRepository.findByPhotoIdForUpdate(photoId)
                .orElseThrow(() -> new CustomException(ErrorCode.BASE_IMAGE_NOT_FOUND));

        String previousStorageKey = lockedBaseImage.getStorageKey();
        lockedBaseImage.replaceStorageKey(newStorageKey);

        List<String> deletedWornImageStorageKeys = wornImageRepository
                .deleteAllByBaseImageId(lockedBaseImage.getId())
                .stream()
                .map(WornImage::getStorageKey)
                .toList();

        return new RegenerationResult(
                toResponse(lockedBaseImage),
                previousStorageKey,
                deletedWornImageStorageKeys
        );
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

    /**
     * 재생성 확정 결과와 정리 대상인 교체 전 기준 이미지·삭제된 착용 이미지들의 저장소 키를 함께 전달한다.
     */
    public record RegenerationResult(
            BaseImageResponse response,
            String previousStorageKey,
            List<String> deletedWornImageStorageKeys
    ) {
    }
}
