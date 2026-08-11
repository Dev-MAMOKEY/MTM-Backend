package com.likelion.mtm.domain.photo.service;

import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.member.repository.MemberRepository;
import com.likelion.mtm.domain.photo.entity.Photo;
import com.likelion.mtm.domain.photo.repository.PhotoRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.likelion.mtm.infra.storage.ImageStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PhotoServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private PhotoRepository photoRepository;

    @Mock
    private ImageStorage imageStorage;

    private PhotoService photoService;

    @BeforeEach
    void setUp() {
        photoService = new PhotoService(
                memberRepository,
                photoRepository,
                imageStorage
        );
    }

    @Test
    @DisplayName("이미지 파일을 업로드하면 저장소와 DB에 저장된다")
    void uploadImage() {
        // given
        Long memberId = 1L;

        Member member = Member.register(
                "test@example.com",
                "encoded-password"
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "photo.jpg",
                "image/jpeg",
                "image-data".getBytes()
        );

        when(memberRepository.findById(memberId))
                .thenReturn(Optional.of(member));

        when(imageStorage.store(file, "photos"))
                .thenReturn("photos/test.jpg");

        when(photoRepository.save(any(Photo.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        when(imageStorage.getUrl("photos/test.jpg"))
                .thenReturn("http://localhost:8080/images/photos/test.jpg");

        // when
        var response = photoService.upload(memberId, file);

        // then
        verify(memberRepository).findById(memberId);
        verify(imageStorage).store(file, "photos");
        verify(photoRepository).save(any(Photo.class));
        verify(imageStorage).getUrl("photos/test.jpg");

        assertThat(response.imageUrl())
                .isEqualTo("http://localhost:8080/images/photos/test.jpg");
    }

    @Test
    @DisplayName("빈 파일을 업로드하면 EMPTY_IMAGE_FILE 예외가 발생한다")
    void uploadEmptyFile() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file",
                "",
                "image/jpeg",
                new byte[0]
        );

        // when & then
        assertThatThrownBy(() -> photoService.upload(1L, emptyFile))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.EMPTY_IMAGE_FILE);

        verifyNoInteractions(
                memberRepository,
                photoRepository,
                imageStorage
        );
    }

    @Test
    @DisplayName("이미지가 아닌 파일을 업로드하면 INVALID_IMAGE_FILE 예외가 발생한다")
    void uploadInvalidFile() {
        // given
        MockMultipartFile invalidFile = new MockMultipartFile(
                "file",
                "document.pptx",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "pptx-data".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> photoService.upload(1L, invalidFile))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_IMAGE_FILE);

        verifyNoInteractions(
                memberRepository,
                photoRepository,
                imageStorage
        );
    }

    @Test
    @DisplayName("회원의 사진첩을 최신순으로 조회한다")
    void getPhotos() {
        // given
        Long memberId = 1L;

        Member member = Member.register(
                "test@example.com",
                "encoded-password"
        );

        Photo oldPhoto = Photo.upload(
                member,
                "photos/old.jpg"
        );

        Photo newPhoto = Photo.upload(
                member,
                "photos/new.jpg"
        );

        when(photoRepository.findAllByMemberIdOrderByCreatedAtDesc(memberId))
                .thenReturn(List.of(newPhoto, oldPhoto));

        when(imageStorage.getUrl("photos/new.jpg"))
                .thenReturn("http://localhost:8080/images/photos/new.jpg");

        when(imageStorage.getUrl("photos/old.jpg"))
                .thenReturn("http://localhost:8080/images/photos/old.jpg");

        // when
        var responses = photoService.getPhotos(memberId);

        // then
        assertThat(responses).hasSize(2);
        assertThat(responses.get(0).imageUrl())
                .isEqualTo("http://localhost:8080/images/photos/new.jpg");
        assertThat(responses.get(1).imageUrl())
                .isEqualTo("http://localhost:8080/images/photos/old.jpg");

        verify(photoRepository)
                .findAllByMemberIdOrderByCreatedAtDesc(memberId);
    }
}
