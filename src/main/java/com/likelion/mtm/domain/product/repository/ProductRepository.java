package com.likelion.mtm.domain.product.repository;

import com.likelion.mtm.domain.product.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 제품의 데이터베이스 접근을 담당하는 리포지토리.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {
}
