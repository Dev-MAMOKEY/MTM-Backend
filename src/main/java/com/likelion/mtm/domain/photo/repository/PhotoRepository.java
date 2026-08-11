package com.likelion.mtm.domain.photo.repository;

import com.likelion.mtm.domain.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

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
}