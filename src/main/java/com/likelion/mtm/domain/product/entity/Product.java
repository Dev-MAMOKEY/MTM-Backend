package com.likelion.mtm.domain.product.entity;

import com.likelion.mtm.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 제품 — MCM 제품. SKU 단위라 같은 모델의 다른 색은 다른 행이다.
 * 크롤링 결과를 일회성으로 적재한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "product",
        uniqueConstraints = @UniqueConstraint(name = "uk_product_sku", columnNames = "sku")
)
public class Product extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** MMHGATA01CO001 형태. 색상까지 구분된 단위. */
    @Column(nullable = false, length = 50)
    private String sku;

    /** SKU 앞 11자 — 사이즈 변형(00S/00M/00L)끼리 제품 컷이 동일하므로 이미지 매칭에 쓴다. */
    @Column(name = "base_sku", nullable = false, length = 20)
    private String baseSku;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(length = 100)
    private String color;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    /** ISO 4217 (KRW) */
    @Column(nullable = false, length = 3)
    private String currency;

    /** Approximately D x W x H inches — 실측 치수 파서의 입력. */
    @Lob
    @Column(columnDefinition = "TEXT")
    private String description;

    @Embedded
    private Dimensions dimensions;

    /** 착용 방식 분류기가 부여한다. 가방 타입이 아니다. */
    @Enumerated(EnumType.STRING)
    @Column(name = "wear_type", nullable = false, length = 20)
    private WearType wearType;

    @Column(name = "detail_url", length = 500)
    private String detailUrl;

    @Builder
    private Product(String sku, String baseSku, String name, String color,
                     BigDecimal price, String currency, String description,
                     Dimensions dimensions, WearType wearType, String detailUrl) {
        this.sku = sku;
        this.baseSku = baseSku;
        this.name = name;
        this.color = color;
        this.price = price;
        this.currency = currency;
        this.description = description;
        this.dimensions = dimensions;
        this.wearType = wearType;
        this.detailUrl = detailUrl;
    }

    /**
     * 크롤링 결과 적재 시 사용하는 정적 팩토리 메서드.
     */
    public static Product of(String sku, String baseSku, String name, String color,
                              BigDecimal price, String currency, String description,
                              Dimensions dimensions, WearType wearType, String detailUrl) {
        return Product.builder()
                .sku(sku)
                .baseSku(baseSku)
                .name(name)
                .color(color)
                .price(price)
                .currency(currency)
                .description(description)
                .dimensions(dimensions)
                .wearType(wearType)
                .detailUrl(detailUrl)
                .build();
    }

    /**
     * 착용 방식 분류기가 재분류한 결과를 반영한다.
     */
    public void classifyWearType(WearType wearType) {
        this.wearType = wearType;
    }
}