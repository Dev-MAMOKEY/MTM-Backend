package com.likelion.mtm.domain.product.dto;

import com.likelion.mtm.domain.product.entity.Currency;
import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.WearType;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        BigDecimal price,
        Currency currency,
        WearType wearType,
        String frontCutUrl
) {
    public static ProductResponse from(Product product, String frontCutUrl) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getPrice(),
                product.getCurrency(),
                product.getWearType(),
                frontCutUrl
        );
    }
}
