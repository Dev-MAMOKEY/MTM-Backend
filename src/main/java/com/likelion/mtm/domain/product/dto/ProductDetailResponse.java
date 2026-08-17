package com.likelion.mtm.domain.product.dto;

import com.likelion.mtm.domain.product.entity.Currency;
import com.likelion.mtm.domain.product.entity.Dimensions;
import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.WearType;

import java.math.BigDecimal;
import java.util.List;

/**
 * 제품 상세 응답 — 목록에는 없는 색상·설명·실측 치수·착용 방식·제품 컷 전체를 담는다.
 * 이미지 URL은 유효기간이 있는 Presigned URL이므로 클라이언트가 오래 캐시하면 안 된다.
 */
public record ProductDetailResponse(
        Long id,
        String sku,
        String name,
        String color,
        BigDecimal price,
        Currency currency,
        String description,
        DimensionsResponse dimensions,
        WearType wearType,
        String detailUrl,
        List<ProductCutResponse> productCuts
) {

    /**
     * 실측 치수 — 크롤링 원문이 인치라 환산 없이 그대로 내려보낸다.
     * 치수를 파싱하지 못한 제품은 이 값이 null이다.
     */
    public record DimensionsResponse(
            BigDecimal depthIn,
            BigDecimal widthIn,
            BigDecimal heightIn
    ) {
        public static DimensionsResponse from(Dimensions dimensions) {
            if (dimensions == null) {
                return null;
            }
            return new DimensionsResponse(
                    dimensions.getDepthIn(),
                    dimensions.getWidthIn(),
                    dimensions.getHeightIn()
            );
        }
    }

    /**
     * 제품 컷 한 장. frontSlot은 목록의 대표 이미지로 쓰이는 정면 컷,
     * wornSlot은 MCM 모델이 실제 착용한 컷이다.
     */
    public record ProductCutResponse(
            Long id,
            Integer slotNo,
            boolean frontSlot,
            boolean wornSlot,
            String imageUrl
    ) {
    }

    /**
     * 제품 컷 URL은 저장소에서 만들어야 하므로 서비스가 완성해 넘겨준다.
     */
    public static ProductDetailResponse from(Product product, List<ProductCutResponse> productCuts) {
        return new ProductDetailResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getColor(),
                product.getPrice(),
                product.getCurrency(),
                product.getDescription(),
                DimensionsResponse.from(product.getDimensions()),
                product.getWearType(),
                product.getDetailUrl(),
                productCuts
        );
    }
}
