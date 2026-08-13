package com.likelion.mtm.domain.worn.service;

import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.photo.entity.BaseImage;
import com.likelion.mtm.domain.photo.entity.Photo;
import com.likelion.mtm.domain.photo.repository.BaseImageRepository;
import com.likelion.mtm.domain.product.entity.Currency;
import com.likelion.mtm.domain.product.entity.Dimensions;
import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.ProductCut;
import com.likelion.mtm.domain.product.entity.WearType;
import com.likelion.mtm.domain.worn.entity.Generator;
import com.likelion.mtm.domain.worn.entity.WornImage;
import com.likelion.mtm.domain.worn.repository.WornImageRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 착용 이미지 저장 확정의 잠금과 중복 방지 정책을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class WornImagePersistenceServiceTest {

    @Mock
    private BaseImageRepository baseImageRepository;

    @Mock
    private WornImageRepository wornImageRepository;

    private WornImagePersistenceService persistenceService;

    @BeforeEach
    void setUp() {
        persistenceService = new WornImagePersistenceService(baseImageRepository, wornImageRepository);
    }

    @Test
    @DisplayName("기준 이미지 행을 잠근 뒤 중복이 없으면 착용 이미지를 저장한다")
    void saveAfterLockingBaseImage() {
        Fixture fixture = fixture();
        when(baseImageRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(fixture.baseImage()));
        when(wornImageRepository.existsByBaseImageIdAndProductId(20L, 30L)).thenReturn(false);
        when(wornImageRepository.save(any(WornImage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        WornImage saved = persistenceService.finalizeCreation(
                20L,
                fixture.product(),
                fixture.productCut(),
                "worn-images/generated",
                Generator.GEMINI
        );

        assertThat(saved.getBaseImage()).isSameAs(fixture.baseImage());
        assertThat(saved.getProduct()).isSameAs(fixture.product());
        assertThat(saved.getProductCut()).isSameAs(fixture.productCut());
        assertThat(saved.getStorageKey()).isEqualTo("worn-images/generated");
        verify(baseImageRepository).findByIdForUpdate(20L);
        verify(wornImageRepository).save(any(WornImage.class));
    }

    @Test
    @DisplayName("잠금 뒤 동일 조합이 발견되면 착용 이미지를 추가 저장하지 않는다")
    void rejectDuplicateAfterLockingBaseImage() {
        Fixture fixture = fixture();
        when(baseImageRepository.findByIdForUpdate(20L))
                .thenReturn(Optional.of(fixture.baseImage()));
        when(wornImageRepository.existsByBaseImageIdAndProductId(20L, 30L)).thenReturn(true);

        assertThatThrownBy(() -> persistenceService.finalizeCreation(
                20L,
                fixture.product(),
                fixture.productCut(),
                "worn-images/generated",
                Generator.GEMINI
        ))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORN_IMAGE_ALREADY_EXISTS);

        verify(wornImageRepository, never()).save(any());
    }

    private Fixture fixture() {
        Member member = Member.register("member@example.com", "password-hash");
        Photo photo = Photo.upload(member, "photos/source");
        BaseImage baseImage = BaseImage.create(photo, "base-images/base");
        ReflectionTestUtils.setField(baseImage, "id", 20L);

        Product product = Product.of(
                "MMHGATA01CO001",
                "MMHGATA01CO",
                "Test Product",
                "Black",
                new BigDecimal("1000000.00"),
                Currency.KRW,
                "Test description",
                Dimensions.of(new BigDecimal("4.25"), new BigDecimal("11.50"), new BigDecimal("8.75")),
                WearType.CROSSBODY,
                "https://example.com/product"
        );
        ReflectionTestUtils.setField(product, "id", 30L);

        ProductCut productCut = ProductCut.of(product, "product-cuts/front", 1, true, false);
        ReflectionTestUtils.setField(productCut, "id", 31L);
        return new Fixture(baseImage, product, productCut);
    }

    /**
     * 저장 확정 테스트에서 함께 사용하는 엔티티 묶음.
     */
    private record Fixture(BaseImage baseImage, Product product, ProductCut productCut) {
    }
}
