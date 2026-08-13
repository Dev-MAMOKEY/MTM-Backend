package com.likelion.mtm.domain.worn.repository;

import com.likelion.mtm.domain.worn.entity.WornImage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 착용 이미지의 데이터베이스 접근을 담당하는 리포지토리.
 */
public interface WornImageRepository extends JpaRepository<WornImage, Long> {

    /**
     * 기준 이미지와 제품 조합에 이미 착용 이미지가 존재하는지 확인한다.
     * 저장 확정 시 잠금 이후의 중복 방지 가드로 쓰인다.
     */
    boolean existsByBaseImageIdAndProductId(Long baseImageId, Long productId);

    /**
     * 기준 이미지와 제품 조합으로 저장된 착용 이미지를 조회한다.
     * "이미 본 제품은 즉시 다시 뜬다" — 있으면 새로 생성하지 않고 이걸 돌려준다.
     */
    Optional<WornImage> findByBaseImageIdAndProductId(Long baseImageId, Long productId);
}
