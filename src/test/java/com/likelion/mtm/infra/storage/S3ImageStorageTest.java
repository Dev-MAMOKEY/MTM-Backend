package com.likelion.mtm.infra.storage;

import com.likelion.mtm.global.exception.CustomException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * S3 이미지 저장소의 바이너리 저장과 조회 변환을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class S3ImageStorageTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    private S3ImageStorage imageStorage;

    @BeforeEach
    void setUp() {
        imageStorage = new S3ImageStorage(s3Client, s3Presigner);
        ReflectionTestUtils.setField(imageStorage, "bucket", "test-bucket");
    }

    @Test
    @DisplayName("이미지 바이트와 MIME 타입을 S3에 저장한다")
    void storeImageData() throws IOException {
        ImageData image = new ImageData("image-data".getBytes(), "image/png");

        String storageKey = imageStorage.store(image, "base-images");

        ArgumentCaptor<PutObjectRequest> requestCaptor =
                ArgumentCaptor.forClass(PutObjectRequest.class);
        ArgumentCaptor<RequestBody> bodyCaptor = ArgumentCaptor.forClass(RequestBody.class);
        verify(s3Client).putObject(requestCaptor.capture(), bodyCaptor.capture());

        assertThat(storageKey).startsWith("base-images/");
        assertThat(requestCaptor.getValue().bucket()).isEqualTo("test-bucket");
        assertThat(requestCaptor.getValue().contentType()).isEqualTo("image/png");
        assertThat(bodyCaptor.getValue().contentStreamProvider().newStream().readAllBytes())
                .isEqualTo("image-data".getBytes());
    }

    @Test
    @DisplayName("웹 업로드 파일은 전체 바이트 변환 없이 입력 스트림으로 저장한다")
    void streamMultipartFile() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("photo.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getSize()).thenReturn(10L);
        when(file.getInputStream()).thenReturn(
                new ByteArrayInputStream("image-data".getBytes())
        );

        String storageKey = imageStorage.store(file, "photos");

        assertThat(storageKey).startsWith("photos/").endsWith(".jpg");
        verify(file).getInputStream();
        verify(file, never()).getBytes();
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @Test
    @DisplayName("S3 객체의 바이트와 Content-Type을 이미지 데이터로 읽는다")
    void loadImageData() {
        ResponseBytes<GetObjectResponse> response = ResponseBytes.fromByteArray(
                GetObjectResponse.builder().contentType("image/jpeg").build(),
                "stored-image".getBytes()
        );
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class))).thenReturn(response);

        ImageData image = imageStorage.load("photos/source");

        assertThat(image.data()).isEqualTo("stored-image".getBytes());
        assertThat(image.mimeType()).isEqualTo("image/jpeg");
    }

    @Test
    @DisplayName("S3 조회 실패는 내부 저장소 예외로 변환한다")
    void convertLoadFailure() {
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenThrow(new RuntimeException("s3 failure"));

        assertThatThrownBy(() -> imageStorage.load("photos/source"))
                .isInstanceOf(CustomException.class);
    }
}
