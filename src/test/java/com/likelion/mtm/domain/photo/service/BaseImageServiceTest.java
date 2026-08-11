package com.likelion.mtm.domain.photo.service;

import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.photo.dto.BaseImageResponse;
import com.likelion.mtm.domain.photo.entity.BaseImage;
import com.likelion.mtm.domain.photo.entity.Photo;
import com.likelion.mtm.domain.photo.repository.BaseImageRepository;
import com.likelion.mtm.domain.photo.repository.PhotoRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.likelion.mtm.infra.imagegen.BaseImagePromptAssembler;
import com.likelion.mtm.infra.imagegen.FakeImageGateway;
import com.likelion.mtm.infra.imagegen.GeneratedImage;
import com.likelion.mtm.infra.storage.ImageData;
import com.likelion.mtm.infra.storage.ImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 기준 이미지 생성 흐름과 저장 확정 정책을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class BaseImageServiceTest {

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private BaseImageRepository baseImageRepository;

    @Mock
    private ImageStorage imageStorage;

    @Mock
    private BaseImagePersistenceService persistenceService;

    private FakeImageGateway imageGenerationGateway;
    private BaseImageService baseImageService;

    @BeforeEach
    void setUp() {
        imageGenerationGateway = new FakeImageGateway(
                new GeneratedImage("generated-image".getBytes(), "image/png")
        );
        baseImageService = new BaseImageService(
                photoRepository,
                persistenceService,
                new BaseImagePromptAssembler(),
                imageGenerationGateway,
                imageStorage
        );
    }

    @Test
    @DisplayName("회원 신체 정보와 원본 사진으로 기준 이미지를 생성하고 저장한다")
    void createBaseImage() {
        Member member = member(1L, true);
        Photo photo = photo(10L, member, "photos/source");
        BaseImageResponse response = response(20L, 10L, "base-url");

        when(photoRepository.findByIdWithMember(10L)).thenReturn(Optional.of(photo));
        when(persistenceService.findExisting(10L)).thenReturn(Optional.empty());
        when(imageStorage.load("photos/source"))
                .thenReturn(new ImageData("source-image".getBytes(), "image/jpeg"));
        when(imageStorage.store(any(ImageData.class), eq("base-images")))
                .thenReturn("base-images/generated");
        when(persistenceService.finalizeCreation(10L, "base-images/generated"))
                .thenReturn(new BaseImagePersistenceService.FinalizationResult(response, true));

        BaseImageResponse result = baseImageService.create(1L, 10L);

        assertThat(result).isEqualTo(response);
        assertThat(imageGenerationGateway.getCallCount()).isEqualTo(1);
        assertThat(imageGenerationGateway.getLastRequest().prompt())
                .contains("175.0 cm", "68.0 kg", "pure white background")
                .contains("Do not remove the background");
        assertThat(imageGenerationGateway.getLastRequest().images()).hasSize(1);

        ArgumentCaptor<ImageData> generatedCaptor = ArgumentCaptor.forClass(ImageData.class);
        verify(imageStorage).store(generatedCaptor.capture(), eq("base-images"));
        assertThat(generatedCaptor.getValue().data()).isEqualTo("generated-image".getBytes());
        assertThat(generatedCaptor.getValue().mimeType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("기준 이미지가 이미 있으면 Gemini를 호출하지 않고 기존 결과를 반환한다")
    void returnExistingBaseImage() {
        Photo photo = photo(10L, member(1L, true), "photos/source");
        BaseImageResponse existing = response(20L, 10L, "existing-url");

        when(photoRepository.findByIdWithMember(10L)).thenReturn(Optional.of(photo));
        when(persistenceService.findExisting(10L)).thenReturn(Optional.of(existing));

        assertThat(baseImageService.create(1L, 10L)).isEqualTo(existing);
        assertThat(imageGenerationGateway.getCallCount()).isZero();
        verifyNoInteractions(imageStorage);
        verify(persistenceService, never()).finalizeCreation(any(), any());
    }

    @Test
    @DisplayName("키나 몸무게가 없으면 Gemini 호출 전에 거부한다")
    void rejectMissingBodyInfo() {
        Photo photo = photo(10L, member(1L, false), "photos/source");
        when(photoRepository.findByIdWithMember(10L)).thenReturn(Optional.of(photo));
        when(persistenceService.findExisting(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baseImageService.create(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BODY_INFO_REQUIRED);

        assertThat(imageGenerationGateway.getCallCount()).isZero();
        verifyNoInteractions(imageStorage);
    }

    @Test
    @DisplayName("존재하지 않는 원본 사진은 거부한다")
    void rejectMissingPhoto() {
        when(photoRepository.findByIdWithMember(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> baseImageService.create(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PHOTO_NOT_FOUND);

        assertThat(imageGenerationGateway.getCallCount()).isZero();
        verifyNoInteractions(imageStorage, persistenceService);
    }

    @Test
    @DisplayName("다른 회원의 원본 사진은 존재하지 않는 사진과 동일하게 거부한다")
    void rejectOtherMembersPhoto() {
        Photo photo = photo(10L, member(2L, true), "photos/source");
        when(photoRepository.findByIdWithMember(10L)).thenReturn(Optional.of(photo));

        assertThatThrownBy(() -> baseImageService.create(1L, 10L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PHOTO_NOT_FOUND);

        assertThat(imageGenerationGateway.getCallCount()).isZero();
        verifyNoInteractions(imageStorage, persistenceService);
    }

    @Test
    @DisplayName("동시 요청이 먼저 저장했으면 경쟁 요청의 이미지를 삭제하고 기존 결과를 반환한다")
    void discardLosingConcurrentImage() {
        Photo photo = photo(10L, member(1L, true), "photos/source");
        BaseImageResponse existing = response(20L, 10L, "existing-url");
        stubGeneration(photo);
        when(persistenceService.finalizeCreation(10L, "base-images/generated"))
                .thenReturn(new BaseImagePersistenceService.FinalizationResult(existing, false));

        assertThat(baseImageService.create(1L, 10L)).isEqualTo(existing);
        verify(imageStorage).delete("base-images/generated");
    }

    @Test
    @DisplayName("저장 확정이 실패하면 새로 저장한 이미지를 정리한다")
    void cleanUpWhenFinalizationFails() {
        Photo photo = photo(10L, member(1L, true), "photos/source");
        stubGeneration(photo);
        when(persistenceService.finalizeCreation(10L, "base-images/generated"))
                .thenThrow(new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> baseImageService.create(1L, 10L))
                .isInstanceOf(CustomException.class);
        verify(imageStorage).delete("base-images/generated");
    }

    @Test
    @DisplayName("저장 확정은 원본 사진을 잠근 뒤 기준 이미지가 없을 때만 저장한다")
    void finalizeOnlyWhenBaseImageDoesNotExist() {
        Photo photo = photo(10L, member(1L, true), "photos/source");
        when(photoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(photo));
        when(baseImageRepository.findByPhotoId(10L)).thenReturn(Optional.empty());
        when(baseImageRepository.save(any(BaseImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(imageStorage.getUrl("base-images/generated")).thenReturn("base-url");

        BaseImagePersistenceService realPersistenceService =
                new BaseImagePersistenceService(photoRepository, baseImageRepository, imageStorage);
        BaseImagePersistenceService.FinalizationResult result =
                realPersistenceService.finalizeCreation(10L, "base-images/generated");

        assertThat(result.created()).isTrue();
        assertThat(result.response().photoId()).isEqualTo(10L);
        verify(photoRepository).findByIdForUpdate(10L);
        verify(baseImageRepository).save(any(BaseImage.class));
    }

    @Test
    @DisplayName("저장 확정 중 기존 기준 이미지가 발견되면 새 행을 저장하지 않는다")
    void finalizeWithExistingBaseImage() {
        Photo photo = photo(10L, member(1L, true), "photos/source");
        BaseImage existing = BaseImage.create(photo, "base-images/existing");
        ReflectionTestUtils.setField(existing, "id", 20L);
        when(photoRepository.findByIdForUpdate(10L)).thenReturn(Optional.of(photo));
        when(baseImageRepository.findByPhotoId(10L)).thenReturn(Optional.of(existing));
        when(imageStorage.getUrl("base-images/existing")).thenReturn("existing-url");

        BaseImagePersistenceService realPersistenceService =
                new BaseImagePersistenceService(photoRepository, baseImageRepository, imageStorage);
        BaseImagePersistenceService.FinalizationResult result =
                realPersistenceService.finalizeCreation(10L, "base-images/generated");

        assertThat(result.created()).isFalse();
        assertThat(result.response().id()).isEqualTo(20L);
        verify(baseImageRepository, never()).save(any(BaseImage.class));
    }

    private void stubGeneration(Photo photo) {
        when(photoRepository.findByIdWithMember(10L)).thenReturn(Optional.of(photo));
        when(persistenceService.findExisting(10L)).thenReturn(Optional.empty());
        when(imageStorage.load("photos/source"))
                .thenReturn(new ImageData("source-image".getBytes(), "image/jpeg"));
        when(imageStorage.store(any(ImageData.class), eq("base-images")))
                .thenReturn("base-images/generated");
    }

    private Member member(Long id, boolean withBodyInfo) {
        Member member = Member.register("member" + id + "@example.com", "password");
        ReflectionTestUtils.setField(member, "id", id);
        if (withBodyInfo) {
            member.updateBodyInfo(new BigDecimal("175.0"), new BigDecimal("68.0"));
        }
        return member;
    }

    private Photo photo(Long id, Member member, String storageKey) {
        Photo photo = Photo.upload(member, storageKey);
        ReflectionTestUtils.setField(photo, "id", id);
        return photo;
    }

    private BaseImageResponse response(Long id, Long photoId, String imageUrl) {
        return new BaseImageResponse(id, photoId, imageUrl, LocalDateTime.now());
    }
}
