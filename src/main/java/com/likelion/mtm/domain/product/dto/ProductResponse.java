package com.likelion.mtm.domain.product.dto;

import com.likelion.mtm.domain.product.entity.Currency;
import com.likelion.mtm.domain.product.entity.Product;

import java.math.BigDecimal;

/**
 * 제품 목록 응답 — 화면에 사진·이름·가격을 띄우는 데 필요한 것만 담는다.
 * frontCutUrl은 유효기간이 있는 Presigned URL이므로 클라이언트가 오래 캐시하면 안 된다.
 */
public record ProductResponse(
        Long id,
        String sku,
        String name,
        BigDecimal price,
        Currency currency,
        String frontCutUrl
) {
    /** 정면 컷이 없는 제품은 frontCutUrl이 null이다 */
    public static ProductResponse from(Product product, String frontCutUrl) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getPrice(),
                product.getCurrency(),
                frontCutUrl
        );
    }
}
