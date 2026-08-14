package com.likelion.mtm.domain.photo.service;

import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.photo.dto.BaseImageResponse;
import com.likelion.mtm.domain.photo.entity.Photo;
import com.likelion.mtm.domain.photo.repository.PhotoRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.likelion.mtm.infra.imagegen.BaseImagePromptAssembler;
import com.likelion.mtm.infra.imagegen.GeneratedImage;
import com.likelion.mtm.infra.imagegen.ImageGenerationGateway;
import com.likelion.mtm.infra.imagegen.ImageGenerationRequest;
import com.likelion.mtm.infra.imagegen.ImageInput;
import com.likelion.mtm.infra.storage.ImageData;
import com.likelion.mtm.infra.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 원본 사진과 회원 신체 정보로 기준 이미지를 생성하는 전체 흐름을 조정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("!('${image-generation.provider:NONE}'.equalsIgnoreCase('NONE'))")
public class BaseImageService {

    private static final String BASE_IMAGE_DIRECTORY = "base-images";

    private final PhotoRepository photoRepository;
    private final BaseImagePersistenceService persistenceService;
    private final BaseImagePromptAssembler promptAssembler;
    private final ImageGenerationGateway imageGenerationGateway;
    private final ImageStorage imageStorage;

    /**
     * 로그인 회원 소유의 원본 사진을 기준 이미지로 만들거나 기존 결과를 반환한다.
     */
    public BaseImageResponse create(Long memberId, Long photoId) {
        Photo photo = findOwnedPhoto(memberId, photoId);

        Optional<BaseImageResponse> existing = persistenceService.findExisting(photoId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Member member = photo.getMember();
        validateBodyInfo(member);

        String newStorageKey = generateBaseImage(photo, member);

        try {
            BaseImagePersistenceService.FinalizationResult result =
                    persistenceService.finalizeCreation(photoId, newStorageKey);

            if (!result.created()) {
                deleteQuietly(newStorageKey);
            }

            return result.response();
        } catch (RuntimeException e) {
            deleteQuietly(newStorageKey);
            throw e;
        }
    }

    /**
     * 원본 사진은 그대로 둔 채 로그인 회원 소유의 기준 이미지만 다시 만든다.
     * 옛 기준 이미지 위의 착용 이미지는 새 얼굴과 맞지 않으므로 함께 삭제한다.
     * 아직 기준 이미지가 없으면 다시 만들 수 없으므로 거부한다.
     */
    public BaseImageResponse regenerate(Long memberId, Long photoId) {
        Photo photo = findOwnedPhoto(memberId, photoId);

        if (persistenceService.findExisting(photoId).isEmpty()) {
            throw new CustomException(ErrorCode.BASE_IMAGE_NOT_FOUND);
        }

        Member member = photo.getMember();
        validateBodyInfo(member);

        String newStorageKey = generateBaseImage(photo, member);

        BaseImagePersistenceService.RegenerationResult result;
        try {
            result = persistenceService.finalizeRegeneration(photoId, newStorageKey);
        } catch (RuntimeException e) {
            deleteQuietly(newStorageKey);
            throw e;
        }

        deleteQuietly(result.previousStorageKey());
        result.deletedWornImageStorageKeys().forEach(this::deleteQuietly);

        return result.response();
    }

    /**
     * 원본 사진과 회원 신체 정보로 기준 이미지를 생성해 저장하고 새 저장소 키를 돌려준다.
     * 생성·신규 저장뿐이고 데이터베이스 확정은 호출자의 몫이다.
     */
    private String generateBaseImage(Photo photo, Member member) {
        ImageData sourceImage = imageStorage.load(photo.getStorageKey());
        String prompt = promptAssembler.assemble(member.getHeightCm(), member.getWeightKg());
        GeneratedImage generatedImage = imageGenerationGateway.generate(
                new ImageGenerationRequest(
                        List.of(new ImageInput(sourceImage.data(), sourceImage.mimeType())),
                        prompt
                )
        );

        return imageStorage.store(
                new ImageData(generatedImage.data(), generatedImage.mimeType()),
                BASE_IMAGE_DIRECTORY
        );
    }

    /**
     * 원본 사진을 조회하고 로그인 회원 소유인지 확인한다.
     */
    private Photo findOwnedPhoto(Long memberId, Long photoId) {
        Photo photo = photoRepository.findByIdWithMember(photoId)
                .orElseThrow(() -> new CustomException(ErrorCode.PHOTO_NOT_FOUND));

        if (!Objects.equals(photo.getMember().getId(), memberId)) {
            throw new CustomException(ErrorCode.PHOTO_NOT_FOUND);
        }

        return photo;
    }

    /**
     * 기준 이미지 프롬프트에 필요한 회원의 키와 몸무게를 검증한다.
     */
    private void validateBodyInfo(Member member) {
        if (member.getHeightCm() == null || member.getWeightKg() == null) {
            throw new CustomException(ErrorCode.BODY_INFO_REQUIRED);
        }
    }

    /**
     * 경쟁 요청 또는 실패한 저장 확정이 남긴 이미지를 원래 결과를 가리지 않도록 정리한다.
     */
    private void deleteQuietly(String storageKey) {
        try {
            imageStorage.delete(storageKey);
        } catch (Exception e) {
            log.error("사용하지 않는 기준 이미지 삭제에 실패했습니다. storageKey={}", storageKey, e);
        }
    }
}
