package com.likelion.mtm.domain.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 실측 치수 — 제품 상세 원문의 "Approximately D x W x H inches"에서 파싱한 실제 크기.
 * 원문이 인치라 그대로 저장한다("사이즈"라 부르지 않는다 — 의류 사이즈와 혼동된다).
 */
@Getter
@Embeddable
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Dimensions {

    @Column(name = "dimension_depth_in", precision = 5, scale = 2)
    private BigDecimal depthIn;

    @Column(name = "dimension_width_in", precision = 5, scale = 2)
    private BigDecimal widthIn;

    @Column(name = "dimension_height_in", precision = 5, scale = 2)
    private BigDecimal heightIn;

    private Dimensions(BigDecimal depthIn, BigDecimal widthIn, BigDecimal heightIn) {
        this.depthIn = depthIn;
        this.widthIn = widthIn;
        this.heightIn = heightIn;
    }

    /**
     * 실측 치수 파서가 파싱 결과를 담을 때 사용하는 정적 팩토리 메서드.
     */
    public static Dimensions of(BigDecimal depthIn, BigDecimal widthIn, BigDecimal heightIn) {
        return new Dimensions(depthIn, widthIn, heightIn);
    }
}