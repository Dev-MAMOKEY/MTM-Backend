package com.likelion.mtm.domain.photo.repository;

import com.likelion.mtm.domain.photo.entity.BaseImage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 기준 이미지의 데이터베이스 접근을 담당하는 리포지토리.
 */
public interface BaseImageRepository extends JpaRepository<BaseImage, Long> {

    /**
     * 착용 이미지 생성 검증에 사용할 원본 사진과 회원 정보를 함께 조회한다.
     *
     * @param baseImageId 기준 이미지 식별자
     * @return 원본 사진과 회원까지 조회된 기준 이미지
     */
    @EntityGraph(attributePaths = {"photo", "photo.member"})
    @Query("select b from BaseImage b where b.id = :baseImageId")
    Optional<BaseImage> findByIdWithPhotoAndMember(@Param("baseImageId") Long baseImageId);

    /**
     * 착용 이미지 저장 확정 동안 기준 이미지 행을 비관적 쓰기 잠금으로 조회한다.
     *
     * @param baseImageId 기준 이미지 식별자
     * @return 잠긴 기준 이미지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BaseImage b where b.id = :baseImageId")
    Optional<BaseImage> findByIdForUpdate(@Param("baseImageId") Long baseImageId);

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
