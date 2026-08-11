package com.likelion.mtm.infra.storage;

import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
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
     * 이미지를 S3에 저장한다.
     */
    @Override
    public String store(MultipartFile file, String directory) {
        String extension = getExtension(file.getOriginalFilename());
        String storageKey = directory + "/" + UUID.randomUUID() + extension;

        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(storageKey)
                    .contentType(file.getContentType())
                    .build();

            s3Client.putObject(
                    request,
                    RequestBody.fromInputStream(
                            file.getInputStream(),
                            file.getSize()
                    )
            );

            return storageKey;

        } catch (IOException e) {
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
     * 파일명에서 확장자를 추출한다.
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
