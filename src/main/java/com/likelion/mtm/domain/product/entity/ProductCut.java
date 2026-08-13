package com.likelion.mtm.domain.product.entity;

import com.likelion.mtm.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 제품 컷 — MCM이 촬영한 제품 사진. SKU 하나에 여러 장이 딸린다.
 * (product, slot_no)가 유니크 — 슬롯 번호는 SKU마다 연속이 아니다(예: 01, 04, 05, 09만 있는 제품도 있다).
 * "SKU당 정면 슬롯은 1장"은 DB로 강제할 수 없다 — MySQL에 부분 UNIQUE 인덱스가 없어서
 * 적재 스크립트가 직접 보장해야 한다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "product_cut",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_product_cut_product_slot",
                columnNames = {"product_id", "slot_no"}
        )
)
public class ProductCut extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Column(name = "slot_no", nullable = false)
    private Integer slotNo;

    /** 제품을 정면에서 찍은 컷 여부 — 목록의 대표 이미지로 쓴다. */
    @Column(name = "is_front_slot", nullable = false)
    private boolean frontSlot;

    /** MCM 모델이 실제 착용한 컷 여부. */
    @Column(name = "is_worn_slot", nullable = false)
    private boolean wornSlot;

    @Builder
    private ProductCut(Product product, String storageKey, Integer slotNo,
                        boolean frontSlot, boolean wornSlot) {
        this.product = product;
        this.storageKey = storageKey;
        this.slotNo = slotNo;
        this.frontSlot = frontSlot;
        this.wornSlot = wornSlot;
    }

    /**
     * 적재 스크립트가 크롤링 결과를 저장할 때 사용하는 정적 팩토리 메서드.
     */
    public static ProductCut of(Product product, String storageKey, Integer slotNo,
                                 boolean frontSlot, boolean wornSlot) {
        return ProductCut.builder()
                .product(product)
                .storageKey(storageKey)
                .slotNo(slotNo)
                .frontSlot(frontSlot)
                .wornSlot(wornSlot)
                .build();
    }
}