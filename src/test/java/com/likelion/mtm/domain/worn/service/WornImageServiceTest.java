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
import com.likelion.mtm.domain.product.repository.ProductCutRepository;
import com.likelion.mtm.domain.product.repository.ProductRepository;
import com.likelion.mtm.domain.worn.dto.WornImageResponse;
import com.likelion.mtm.domain.worn.entity.Generator;
import com.likelion.mtm.domain.worn.entity.WornImage;
import com.likelion.mtm.domain.worn.repository.WornImageRepository;
import com.likelion.mtm.global.config.ImageGenerationConfig;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.likelion.mtm.infra.imagegen.FakeImageGateway;
import com.likelion.mtm.infra.imagegen.GeneratedImage;
import com.likelion.mtm.infra.imagegen.ImageGenerationProvider;
import com.likelion.mtm.infra.imagegen.ImageGenerationRequest;
import com.likelion.mtm.infra.imagegen.WornImagePromptAssembler;
import com.likelion.mtm.infra.storage.ImageData;
import com.likelion.mtm.infra.storage.ImageStorage;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 착용 이미지 생성 흐름의 검증, 입력 순서와 실패 시 저장소 정리 정책을 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class WornImageServiceTest {

    @Mock
    private BaseImageRepository baseImageRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ProductCutRepository productCutRepository;

    @Mock
    private WornImageRepository wornImageRepository;

    @Mock
    private WornImagePersistenceService persistenceService;

    @Mock
    private WornImagePromptAssembler promptAssembler;

    @Mock
    private ImageStorage imageStorage;

    private FakeImageGateway imageGenerationGateway;
    private WornImageService wornImageService;

    @BeforeEach
    void setUp() {
        imageGenerationGateway = new FakeImageGateway(
                new GeneratedImage("generated-worn-image".getBytes(), "image/png")
        );
        wornImageService = new WornImageService(
                baseImageRepository,
                productRepository,
                productCutRepository,
                wornImageRepository,
                persistenceService,
                promptAssembler,
                imageGenerationGateway,
                imageStorage,
                new ImageGenerationConfig.Properties(ImageGenerationProvider.GEMINI)
        );
    }

    @Test
    @DisplayName("다른 회원의 기준 이미지는 존재하지 않는 기준 이미지와 동일하게 거부한다")
    void rejectOtherMembersBaseImage() {
        BaseImage baseImage = baseImage(20L, member(2L, true));
        when(baseImageRepository.findByIdWithPhotoAndMember(20L))
                .thenReturn(Optional.of(baseImage));

        assertThatThrownBy(() -> wornImageService.create(1L, 20L, 30L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.BASE_IMAGE_NOT_FOUND);

        verifyNoInteractions(productRepository, productCutRepository, imageStorage, persistenceService);
        assertThat(imageGenerationGateway.getCallCount()).isZero();
    }

    @Test
    @DisplayName("회원 신체 정보가 없으면 이미지 생성 전에 거부한다")
    void rejectMissingBodyInfo() {
        GenerationFixture fixture = stubEntities(false, dimensions(), WearType.CROSSBODY);

        assertThatThrownBy(() -> wornImageService.create(1L, 20L, 30L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.WORN_IMAGE_BODY_INFO_REQUIRED);

        verifyNoInteractions(imageStorage, persistenceService);
        assertThat(imageGenerationGateway.getCallCount()).isZero();
    }

    @Test
    @DisplayName("존재하지 않는 제품은 이미지 생성 전에 거부한다")
    void rejectMissingProduct() {
        BaseImage baseImage = baseImage(20L, member(1L, true));
        when(baseImageRepository.findByIdWithPhotoAndMember(20L)).thenReturn(Optional.of(baseImage));
        when(wornImageRepository.findByBaseImageIdAndProductId(20L, 30L)).thenReturn(Optional.empty());
        when(productRepository.findById(30L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> wornImageService.create(1L, 20L, 30L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_NOT_FOUND);

        verifyNoInteractions(productCutRepository, imageStorage, persistenceService);
        assertThat(imageGenerationGateway.getCallCount()).isZero();
    }

    @Test
    @DisplayName("제품 실측 치수가 없으면 이미지 생성 전에 거부한다")
    void rejectMissingDimensions() {
        stubEntities(true, null, WearType.CROSSBODY);

        assertThatThrownBy(() -> wornImageService.create(1L, 20L, 30L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_DIMENSIONS_REQUIRED);

        verifyNoInteractions(imageStorage, persistenceService);
        assertThat(imageGenerationGateway.getCallCount()).isZero();
    }

    @Test
    @DisplayName("제품 착용 방식이 없으면 이미지 생성 전에 거부한다")
    void rejectMissingWearType() {
        stubEntities(true, dimensions(), null);

        assertThatThrownBy(() -> wornImageService.create(1L, 20L, 30L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_WEAR_TYPE_REQUIRED);

        verifyNoInteractions(imageStorage, persistenceService);
        assertThat(imageGenerationGateway.getCallCount()).isZero();
    }

    @Test
    @DisplayName("정면 제품 컷이 없으면 이미지 생성 전에 거부한다")
    void rejectMissingProductCut() {
        Member member = member(1L, true);
        BaseImage baseImage = baseImage(20L, member);
        Product product = product(30L, dimensions(), WearType.CROSSBODY);
        when(baseImageRepository.findByIdWithPhotoAndMember(20L)).thenReturn(Optional.of(baseImage));
        when(wornImageRepository.findByBaseImageIdAndProductId(20L, 30L)).thenReturn(Optional.empty());
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
        when(productCutRepository.findFirstByProductIdAndFrontSlotTrueOrderBySlotNoAsc(30L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> wornImageService.create(1L, 20L, 30L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.PRODUCT_CUT_NOT_FOUND);

        verifyNoInteractions(imageStorage, persistenceService);
        assertThat(imageGenerationGateway.getCallCount()).isZero();
    }

    @Test
    @DisplayName("이미 같은 조합의 착용 이미지가 있으면 새로 생성하지 않고 저장된 것을 즉시 반환한다")
    void reuseExistingWornImage() {
        Member member = member(1L, true);
        BaseImage baseImage = baseImage(20L, member);
        Product product = product(30L, dimensions(), WearType.CROSSBODY);
        ProductCut productCut = productCut(31L, product);
        WornImage existingWornImage = WornImage.create(
                baseImage,
                product,
                "worn-images/existing",
                Generator.GEMINI,
                productCut
        );
        ReflectionTestUtils.setField(existingWornImage, "id", 40L);

        when(baseImageRepository.findByIdWithPhotoAndMember(20L)).thenReturn(Optional.of(baseImage));
        when(wornImageRepository.findByBaseImageIdAndProductId(20L, 30L))
                .thenReturn(Optional.of(existingWornImage));
        when(imageStorage.getUrl("worn-images/existing")).thenReturn("existing-worn-image-url");

        WornImageResponse response = wornImageService.create(1L, 20L, 30L);

        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.imageUrl()).isEqualTo("existing-worn-image-url");
        assertThat(response.generator()).isEqualTo(Generator.GEMINI);

        verifyNoInteractions(productRepository, productCutRepository, promptAssembler, persistenceService);
        verify(imageStorage, never()).load(anyString());
        verify(imageStorage, never()).store(any(ImageData.class), anyString());
        assertThat(imageGenerationGateway.getCallCount()).isZero();
    }

    @Test
    @DisplayName("기준 이미지와 제품 컷 순서 및 조립된 프롬프트로 착용 이미지를 생성하고 저장한다")
    void createWornImage() {
        GenerationFixture fixture = stubEntities(true, dimensions(), WearType.CROSSBODY);
        when(imageStorage.load("base-images/base")).thenReturn(
                new ImageData("base-image".getBytes(), "image/jpeg")
        );
        when(imageStorage.load("product-cuts/front")).thenReturn(
                new ImageData("product-cut".getBytes(), "image/png")
        );
        when(promptAssembler.assemble(fixture.member(), fixture.product()))
                .thenReturn("assembled-worn-prompt");
        when(imageStorage.store(any(ImageData.class), eq("worn-images")))
                .thenReturn("worn-images/generated");

        WornImage savedWornImage = WornImage.create(
                fixture.baseImage(),
                fixture.product(),
                "worn-images/generated",
                Generator.GEMINI,
                fixture.productCut()
        );
        ReflectionTestUtils.setField(savedWornImage, "id", 40L);
        when(persistenceService.finalizeCreation(
                20L,
                fixture.product(),
                fixture.productCut(),
                "worn-images/generated",
                Generator.GEMINI
        )).thenReturn(savedWornImage);
        when(imageStorage.getUrl("worn-images/generated")).thenReturn("worn-image-url");

        WornImageResponse response = wornImageService.create(1L, 20L, 30L);

        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.imageUrl()).isEqualTo("worn-image-url");
        assertThat(response.generator()).isEqualTo(Generator.GEMINI);

        ImageGenerationRequest request = imageGenerationGateway.getLastRequest();
        assertThat(request.prompt()).isEqualTo("assembled-worn-prompt");
        assertThat(request.images()).hasSize(2);
        assertThat(request.images().get(0).data()).isEqualTo("base-image".getBytes());
        assertThat(request.images().get(1).data()).isEqualTo("product-cut".getBytes());

        verify(imageStorage).store(any(ImageData.class), eq("worn-images"));
        var finalizationOrder = inOrder(imageStorage, persistenceService);
        finalizationOrder.verify(imageStorage).getUrl("worn-images/generated");
        finalizationOrder.verify(persistenceService).finalizeCreation(
                20L,
                fixture.product(),
                fixture.productCut(),
                "worn-images/generated",
                Generator.GEMINI
        );
        verify(imageStorage, never()).delete(any());
    }

    @Test
    @DisplayName("이미지 URL 생성이 실패하면 새 저장소 객체를 삭제하고 DB 저장을 확정하지 않는다")
    void cleanUpWhenImageUrlGenerationFails() {
        GenerationFixture fixture = stubEntities(true, dimensions(), WearType.CROSSBODY);
        stubGeneration(fixture);
        when(imageStorage.getUrl("worn-images/generated"))
                .thenThrow(new CustomException(ErrorCode.IMAGE_STORAGE_ERROR));

        assertThatThrownBy(() -> wornImageService.create(1L, 20L, 30L))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_STORAGE_ERROR);

        verify(imageStorage).delete("worn-images/generated");
        verifyNoInteractions(persistenceService);
    }

    @Test
    @DisplayName("데이터베이스 저장 확정이 실패하면 새로 저장한 착용 이미지를 삭제한다")
    void cleanUpWhenFinalizationFails() {
        GenerationFixture fixture = stubEntities(true, dimensions(), WearType.CROSSBODY);
        stubGeneration(fixture);
        when(imageStorage.getUrl("worn-images/generated")).thenReturn("worn-image-url");
        when(persistenceService.finalizeCreation(
                20L,
                fixture.product(),
                fixture.productCut(),
                "worn-images/generated",
                Generator.GEMINI
        )).thenThrow(new CustomException(ErrorCode.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> wornImageService.create(1L, 20L, 30L))
                .isInstanceOf(CustomException.class);

        verify(imageStorage).delete("worn-images/generated");
    }

    /**
     * 착용 이미지 생성 전 조회되는 기준 이미지, 제품과 정면 제품 컷을 준비한다.
     */
    private GenerationFixture stubEntities(
            boolean withBodyInfo,
            Dimensions dimensions,
            WearType wearType
    ) {
        Member member = member(1L, withBodyInfo);
        BaseImage baseImage = baseImage(20L, member);
        Product product = product(30L, dimensions, wearType);
        ProductCut productCut = productCut(31L, product);

        when(baseImageRepository.findByIdWithPhotoAndMember(20L)).thenReturn(Optional.of(baseImage));
        when(wornImageRepository.findByBaseImageIdAndProductId(20L, 30L)).thenReturn(Optional.empty());
        when(productRepository.findById(30L)).thenReturn(Optional.of(product));
        when(productCutRepository.findFirstByProductIdAndFrontSlotTrueOrderBySlotNoAsc(30L))
                .thenReturn(Optional.of(productCut));

        return new GenerationFixture(member, baseImage, product, productCut);
    }

    /**
     * 외부 이미지 읽기, 프롬프트 조립과 생성 이미지 저장 결과를 준비한다.
     */
    private void stubGeneration(GenerationFixture fixture) {
        when(imageStorage.load("base-images/base"))
                .thenReturn(new ImageData("base-image".getBytes(), "image/jpeg"));
        when(imageStorage.load("product-cuts/front"))
                .thenReturn(new ImageData("product-cut".getBytes(), "image/png"));
        when(promptAssembler.assemble(fixture.member(), fixture.product()))
                .thenReturn("assembled-worn-prompt");
        when(imageStorage.store(any(ImageData.class), eq("worn-images")))
                .thenReturn("worn-images/generated");
    }

    private Member member(Long id, boolean withBodyInfo) {
        Member member = Member.register("member" + id + "@example.com", "password-hash");
        ReflectionTestUtils.setField(member, "id", id);
        if (withBodyInfo) {
            member.updateBodyInfo(new BigDecimal("175.0"), new BigDecimal("68.0"));
        }
        return member;
    }

    private BaseImage baseImage(Long id, Member member) {
        Photo photo = Photo.upload(member, "photos/source");
        ReflectionTestUtils.setField(photo, "id", 10L);
        BaseImage baseImage = BaseImage.create(photo, "base-images/base");
        ReflectionTestUtils.setField(baseImage, "id", id);
        return baseImage;
    }

    private Product product(Long id, Dimensions dimensions, WearType wearType) {
        Product product = Product.of(
                "MMHGATA01CO001",
                "MMHGATA01CO",
                "Test Product",
                "Black",
                new BigDecimal("1000000.00"),
                Currency.KRW,
                "Test description",
                dimensions,
                wearType,
                "https://example.com/product"
        );
        ReflectionTestUtils.setField(product, "id", id);
        return product;
    }

    private ProductCut productCut(Long id, Product product) {
        ProductCut productCut = ProductCut.of(product, "product-cuts/front", 1, true, false);
        ReflectionTestUtils.setField(productCut, "id", id);
        return productCut;
    }

    private Dimensions dimensions() {
        return Dimensions.of(
                new BigDecimal("4.25"),
                new BigDecimal("11.50"),
                new BigDecimal("8.75")
        );
    }

    /**
     * 한 테스트에서 함께 사용하는 착용 이미지 생성 대상 엔티티 묶음.
     */
    private record GenerationFixture(
            Member member,
            BaseImage baseImage,
            Product product,
            ProductCut productCut
    ) {
    }
}
