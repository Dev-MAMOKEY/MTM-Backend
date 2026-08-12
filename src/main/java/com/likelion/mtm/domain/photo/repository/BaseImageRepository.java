package com.likelion.mtm.domain.photo.repository;

import com.likelion.mtm.domain.photo.entity.BaseImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 기준 이미지의 데이터베이스 접근을 담당하는 리포지토리.
 */
public interface BaseImageRepository extends JpaRepository<BaseImage, Long> {

    /**
     * 원본 사진에 연결된 기준 이미지를 조회한다.
     *
     * @param photoId 원본 사진 식별자
     * @return 기준 이미지. 아직 생성되지 않았으면 빈 값
     */
    Optional<BaseImage> findByPhotoId(Long photoId);

    /**
     * 여러 원본 사진에 연결된 기준 이미지를 한 번에 조회한다.
     *
     * @param photoIds 원본 사진 식별자 목록
     * @return 조회된 기준 이미지 목록
     */
    List<BaseImage> findAllByPhotoIdIn(Collection<Long> photoIds);
}
