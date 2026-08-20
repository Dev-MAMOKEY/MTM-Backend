package com.likelion.mtm.domain.product.repository;

import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.ProductCut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
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

    /**
     * 목록의 대표 이미지용 — 여러 제품의 정면 컷을 한 번에 가져온다.
     * 제품마다 따로 조회하면 N+1이 나므로, 조회 결과를 Map으로 묶어 쓴다.
     */
    List<ProductCut> findAllByProductInAndFrontSlotIsTrue(List<Product> products);

    /**
     * 상세 조회용 — 제품 하나의 컷을 슬롯 번호 순으로 전부 가져온다.
     * 촬영 순서대로 보여주기 위해 슬롯 번호로 정렬한다.
     *
     * @param productId 제품 식별자
     * @return 슬롯 번호 오름차순 제품 컷 목록
     */
    List<ProductCut> findAllByProductIdOrderBySlotNoAsc(Long productId);
}