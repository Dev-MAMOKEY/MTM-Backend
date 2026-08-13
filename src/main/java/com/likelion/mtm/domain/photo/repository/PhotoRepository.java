package com.likelion.mtm.domain.photo.repository;

import com.likelion.mtm.domain.photo.entity.Photo;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 원본 사진의 데이터베이스 접근을 담당하는 리포지토리.
 */
public interface PhotoRepository extends JpaRepository<Photo, Long> {

    /**
     * 특정 회원이 업로드한 원본 사진을 최신순으로 조회한다.
     *
     * @param memberId 회원 식별자
     * @return 해당 회원의 원본 사진 목록
     */
    List<Photo> findAllByMemberIdOrderByCreatedAtDesc(Long memberId);

    /**
     * 기준 이미지 생성 검증에 사용할 회원 정보를 함께 조회한다.
     *
     * @param photoId 원본 사진 식별자
     * @return 회원까지 조회된 원본 사진
     */
    @EntityGraph(attributePaths = "member")
    @Query("select p from Photo p where p.id = :photoId")
    Optional<Photo> findByIdWithMember(@Param("photoId") Long photoId);

    /**
     * 기준 이미지 저장 확정 동안 원본 사진 행을 비관적 쓰기 잠금으로 조회한다.
     *
     * @param photoId 원본 사진 식별자
     * @return 잠긴 원본 사진
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Photo p where p.id = :photoId")
    Optional<Photo> findByIdForUpdate(@Param("photoId") Long photoId);
}
