package com.likelion.mtm.domain.product.repository;

import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.ProductCut;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 제품 컷의 데이터베이스 접근을 담당하는 리포지토리.
 */
public interface ProductCutRepository extends JpaRepository<ProductCut, Long> {

    /**
     * 목록의 대표 이미지용 — 여러 제품의 정면 컷을 한 번에 가져온다.
     * 제품마다 따로 조회하면 N+1이 나므로, 조회 결과를 Map으로 묶어 쓴다.
     */
    List<ProductCut> findAllByProductInAndFrontSlotIsTrue(List<Product> products);
}
