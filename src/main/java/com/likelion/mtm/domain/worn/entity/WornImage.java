package com.likelion.mtm.domain.worn.entity;

import com.likelion.mtm.domain.photo.entity.BaseImage;
import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.ProductCut;
import com.likelion.mtm.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
 * 착용 이미지 — 기준 이미지에 제품이 올라간 최종 결과물. 회원이 화면에서 보는 것.
 * (base_image, product)가 유니크 — "이미 본 제품은 즉시 다시 뜬다"의 실체다.
 * 같은 조합이 이미 있으면 새로 생성하지 않고 저장된 걸 돌려준다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "worn_image",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_worn_image_base_product",
                columnNames = {"base_image_id", "product_id"}
        )
)
public class WornImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 바탕 기준 이미지 — 기준 이미지를 다시 만들면 함께 삭제된다(서비스 계층에서 처리). */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "base_image_id", nullable = false)
    private BaseImage baseImage;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 다시 만들기는 새 행이 아니라 이 값을 교체한다. */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    /** GEMINI / OPENAI / FAKE — 두 AI 결과를 비교하려면 필요하다. */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Generator generator;

    /** 사용한 제품 컷 — 어떤 컷으로 그렸는지 추적용(선택). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_cut_id")
    private ProductCut productCut;

    @Builder
    private WornImage(BaseImage baseImage, Product product, String storageKey,
                       Generator generator, ProductCut productCut) {
        this.baseImage = baseImage;
        this.product = product;
        this.storageKey = storageKey;
        this.generator = generator;
        this.productCut = productCut;
    }

    /**
     * 최초 생성 시 사용하는 정적 팩토리 메서드.
     */
    public static WornImage create(BaseImage baseImage, Product product, String storageKey,
                                    Generator generator, ProductCut productCut) {
        return WornImage.builder()
                .baseImage(baseImage)
                .product(product)
                .storageKey(storageKey)
                .generator(generator)
                .productCut(productCut)
                .build();
    }

    /**
     * 다시 만들기 — 새 행을 만들지 않고 저장소 키(와 생성 정보)를 교체한다.
     */
    public void replaceImage(String newStorageKey, Generator generator, ProductCut productCut) {
        this.storageKey = newStorageKey;
        this.generator = generator;
        this.productCut = productCut;
    }
}