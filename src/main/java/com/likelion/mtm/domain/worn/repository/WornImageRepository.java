package com.likelion.mtm.domain.worn.repository;

import com.likelion.mtm.domain.worn.entity.WornImage;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 착용 이미지의 데이터베이스 접근을 담당하는 리포지토리.
 */
public interface WornImageRepository extends JpaRepository<WornImage, Long> {

    /**
     * 기준 이미지와 제품 조합에 이미 착용 이미지가 존재하는지 확인한다.
     */
    boolean existsByBaseImageIdAndProductId(Long baseImageId, Long productId);
}
