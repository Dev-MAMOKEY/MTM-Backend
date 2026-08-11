package com.likelion.mtm.infra.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 저장소 추상화 인터페이스.
 * 이미지 저장소 구현을 동일한 방식으로 사용할 수 있게 한다.
 */
public interface ImageStorage {

    /**
     * 이미지를 저장하고 저장소에서 파일을 식별할 수 있는 키를 반환한다.
     *
     * @param file 저장할 이미지 파일
     * @param directory 저장할 디렉터리 구분값
     * @return 저장된 이미지의 storage key
     */
    String store(MultipartFile file, String directory);

    /**
     * storage key에 해당하는 이미지의 접근 URL을 반환한다.
     *
     * @param storageKey 저장소 키
     * @return 이미지 접근 URL
     */
    String getUrl(String storageKey);

    /**
     * storage key에 해당하는 이미지를 삭제한다.
     *
     * @param storageKey 저장소 키
     */
    void delete(String storageKey);
}
