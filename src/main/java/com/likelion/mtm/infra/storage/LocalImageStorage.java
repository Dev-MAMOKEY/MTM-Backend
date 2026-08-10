package com.likelion.mtm.infra.storage;

import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 개발 환경에서 이미지를 로컬 파일 시스템에 저장하는 구현체.
 */
@Component
@Profile("local")
public class LocalImageStorage implements ImageStorage {

    private final Path rootPath;
    private final String baseUrl;

    public LocalImageStorage(
            @Value("${image.storage.local.root-path}") String rootPath,
            @Value("${image.storage.local.base-url}") String baseUrl
    ) {
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
        this.baseUrl = baseUrl;
    }

    /**
     * 이미지를 로컬 파일 시스템에 저장하고 storage key를 반환한다.
     */
    @Override
    public String store(MultipartFile file, String directory) {
        String extension = extractExtension(file.getOriginalFilename());
        String filename = UUID.randomUUID() + extension;
        String storageKey = directory + "/" + filename;

        Path targetPath = rootPath.resolve(storageKey).normalize();

        try {
            Files.createDirectories(targetPath.getParent());

            Files.copy(
                    file.getInputStream(),
                    targetPath,
                    StandardCopyOption.REPLACE_EXISTING
            );

            return storageKey;
        } catch (IOException e) {
            // 프로젝트의 적절한 ErrorCode로 교체한다.
            throw new IllegalStateException("이미지 저장에 실패했습니다.", e);
        }
    }

    /**
     * storage key를 로컬 이미지 접근 URL로 변환한다.
     */
    @Override
    public String getUrl(String storageKey) {
        return baseUrl + "/" + storageKey;
    }

    /**
     * 로컬 파일 시스템에서 이미지를 삭제한다.
     */
    @Override
    public void delete(String storageKey) {
        Path targetPath = rootPath.resolve(storageKey).normalize();

        try {
            Files.deleteIfExists(targetPath);
        } catch (IOException e) {
            // 프로젝트의 적절한 ErrorCode로 교체한다.
            throw new IllegalStateException("이미지 삭제에 실패했습니다.", e);
        }
    }

    /**
     * 원본 파일명에서 확장자를 추출한다.
     */
    private String extractExtension(String originalFilename) {
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