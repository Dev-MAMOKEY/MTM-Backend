package com.likelion.mtm.infra.storage;

import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.io.IOException;
import java.time.Duration;
import java.util.UUID;

/**
 * AWS S3 기반 이미지 저장소.
 */
@Component
@RequiredArgsConstructor
public class S3ImageStorage implements ImageStorage {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    /**
     * 웹 업로드 이미지를 메모리에 모두 올리지 않고 S3에 스트리밍한다.
     */
    @Override
    public String store(MultipartFile file, String directory) {
        String storageKey = directory + "/" + UUID.randomUUID() + getExtension(file.getOriginalFilename());

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );

            return storageKey;
        } catch (IOException e) {
            throw new CustomException(ErrorCode.IMAGE_STORAGE_ERROR);
        }
    }

    /**
     * 이미 바이트로 존재하는 생성 이미지를 S3에 저장한다.
     */
    @Override
    public String store(ImageData image, String directory) {
        String storageKey = directory + "/" + UUID.randomUUID();

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .contentType(image.mimeType())
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromBytes(image.data())
            );

            return storageKey;

        } catch (Exception e) {
            throw new CustomException(ErrorCode.IMAGE_STORAGE_ERROR);
        }
    }

    /**
     * S3 객체의 바이너리와 Content-Type을 읽는다.
     */
    @Override
    public ImageData load(String storageKey) {
        try {
            GetObjectRequest request = GetObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();

            ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(request);

            return new ImageData(
                    response.asByteArray(),
                    response.response().contentType()
            );
        } catch (Exception e) {
            throw new CustomException(ErrorCode.IMAGE_STORAGE_ERROR);
        }
    }

    /**
     * 저장된 이미지에 일정 시간 접근할 수 있는 Presigned URL을 생성한다.
     */
    @Override
    public String getUrl(String storageKey) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(storageKey)
                .build();

        GetObjectPresignRequest presignRequest =
                GetObjectPresignRequest.builder()
                        .signatureDuration(Duration.ofMinutes(30))
                        .getObjectRequest(getObjectRequest)
                        .build();

        return s3Presigner.presignGetObject(presignRequest)
                .url()
                .toString();
    }

    /**
     * S3에서 이미지를 삭제한다.
     */
    @Override
    public void delete(String storageKey) {
        try {
            DeleteObjectRequest request = DeleteObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .build();

            s3Client.deleteObject(request);

        } catch (Exception e) {
            throw new CustomException(ErrorCode.IMAGE_STORAGE_ERROR);
        }
    }

    /**
     * 업로드 파일명에서 확장자를 추출한다.
     */
    private String getExtension(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }

        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex < 0) {
            return "";
        }

        return originalFilename.substring(dotIndex);
    }
}
