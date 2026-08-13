package com.likelion.mtm.domain.product.service;

import com.likelion.mtm.domain.product.dto.ProductResponse;
import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.ProductCut;
import com.likelion.mtm.domain.product.repository.ProductCutRepository;
import com.likelion.mtm.domain.product.repository.ProductRepository;
import com.likelion.mtm.infra.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 제품 조회 서비스.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCutRepository productCutRepository;
    private final ImageStorage imageStorage;

    /**
     * 제품 목록 조회. 제품이 30개 규모라 전체를 한 번에 준다.
     * DTO 변환까지 트랜잭션 안에서 끝낸다.
     */
    public List<ProductResponse> getProducts() {
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) {
            return List.of();
        }

        // 제품마다 정면 컷을 따로 조회하면 N+1이 난다. 한 번에 가져와 Map으로 묶는다
        Map<Long, String> frontCutKeys = productCutRepository
                .findAllByProductInAndFrontSlotIsTrue(products)
                .stream()
                .collect(Collectors.toMap(
                        cut -> cut.getProduct().getId(),
                        ProductCut::getStorageKey,
                        // 정면 컷이 여러 장이면 먼저 나온 것을 대표로 쓴다
                        (first, second) -> first
                ));

        return products.stream()
                .map(product -> ProductResponse.from(product, frontCutUrl(frontCutKeys, product)))
                .toList();
    }

    /** 정면 컷이 없는 제품은 URL 없이 내려보낸다 — 목록에서 이미지 자리만 비게 한다 */
    private String frontCutUrl(Map<Long, String> frontCutKeys, Product product) {
        String storageKey = frontCutKeys.get(product.getId());
        if (storageKey == null) {
            return null;
        }
        return imageStorage.getUrl(storageKey);
    }
}
