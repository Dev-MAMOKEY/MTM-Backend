package com.likelion.mtm.domain.product.repository;

import com.likelion.mtm.domain.product.entity.ProductCut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 제품 컷의 데이터베이스 접근을 담당하는 리포지토리.
 */
public interface ProductCutRepository extends JpaRepository<ProductCut, Long> {

    /**
     * 제품의 정면 컷 중 슬롯 번호가 가장 작은 한 장을 생성 입력으로 선택한다.
     *
     * @param productId 제품 식별자
     * @return 선택된 정면 제품 컷
     */
    Optional<ProductCut> findFirstByProductIdAndFrontSlotTrueOrderBySlotNoAsc(Long productId);
}
