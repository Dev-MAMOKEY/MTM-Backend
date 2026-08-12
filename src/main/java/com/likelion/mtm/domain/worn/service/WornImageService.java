package com.likelion.mtm.domain.worn.service;

import com.likelion.mtm.domain.member.entity.Member;
import com.likelion.mtm.domain.photo.entity.BaseImage;
import com.likelion.mtm.domain.photo.repository.BaseImageRepository;
import com.likelion.mtm.domain.product.entity.Dimensions;
import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.ProductCut;
import com.likelion.mtm.domain.product.repository.ProductCutRepository;
import com.likelion.mtm.domain.product.repository.ProductRepository;
import com.likelion.mtm.domain.worn.dto.WornImageResponse;
import com.likelion.mtm.domain.worn.entity.Generator;
import com.likelion.mtm.domain.worn.entity.WornImage;
import com.likelion.mtm.domain.worn.repository.WornImageRepository;
import com.likelion.mtm.global.config.ImageGenerationConfig;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import com.likelion.mtm.infra.imagegen.GeneratedImage;
import com.likelion.mtm.infra.imagegen.ImageGenerationGateway;
import com.likelion.mtm.infra.imagegen.ImageGenerationProvider;
import com.likelion.mtm.infra.imagegen.ImageGenerationRequest;
import com.likelion.mtm.infra.imagegen.ImageInput;
import com.likelion.mtm.infra.imagegen.WornImagePromptAssembler;
import com.likelion.mtm.infra.storage.ImageData;
import com.likelion.mtm.infra.storage.ImageStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 기준 이미지와 제품 컷으로 착용 이미지를 생성하는 전체 흐름을 조정한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnExpression("!('${image-generation.provider:NONE}'.equalsIgnoreCase('NONE'))")
public class WornImageService {

    private static final String WORN_IMAGE_DIRECTORY = "worn-images";

    private final BaseImageRepository baseImageRepository;
    private final ProductRepository productRepository;
    private final ProductCutRepository productCutRepository;
    private final WornImageRepository wornImageRepository;
    private final WornImagePersistenceService persistenceService;
    private final WornImagePromptAssembler promptAssembler;
    private final ImageGenerationGateway imageGenerationGateway;
    private final ImageStorage imageStorage;
    private final ImageGenerationConfig.Properties imageGenerationProperties;

    /**
     * 로그인 회원의 기준 이미지에 선택한 제품을 착용한 이미지를 생성하고 저장한다.
     */
    public WornImageResponse create(Long memberId, Long baseImageId, Long productId) {
        BaseImage baseImage = findOwnedBaseImage(memberId, baseImageId);

        if (wornImageRepository.existsByBaseImageIdAndProductId(baseImageId, productId)) {
            throw new CustomException(ErrorCode.WORN_IMAGE_ALREADY_EXISTS);
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_NOT_FOUND));
        ProductCut productCut = productCutRepository
                .findFirstByProductIdAndFrontSlotTrueOrderBySlotNoAsc(productId)
                .orElseThrow(() -> new CustomException(ErrorCode.PRODUCT_CUT_NOT_FOUND));

        Member member = baseImage.getPhoto().getMember();
        validateGenerationData(member, product);

        ImageData baseImageData = imageStorage.load(baseImage.getStorageKey());
        ImageData productCutData = imageStorage.load(productCut.getStorageKey());
        String prompt = promptAssembler.assemble(member, product);
        GeneratedImage generatedImage = imageGenerationGateway.generate(
                new ImageGenerationRequest(
                        List.of(
                                new ImageInput(baseImageData.data(), baseImageData.mimeType()),
                                new ImageInput(productCutData.data(), productCutData.mimeType())
                        ),
                        prompt
                )
        );

        String newStorageKey = imageStorage.store(
                new ImageData(generatedImage.data(), generatedImage.mimeType()),
                WORN_IMAGE_DIRECTORY
        );

        WornImage savedWornImage;
        try {
            savedWornImage = persistenceService.finalizeCreation(
                    baseImageId,
                    product,
                    productCut,
                    newStorageKey,
                    currentGenerator()
            );
        } catch (RuntimeException e) {
            deleteQuietly(newStorageKey);
            throw e;
        }

        return WornImageResponse.from(
                savedWornImage,
                imageStorage.getUrl(savedWornImage.getStorageKey())
        );
    }

    /**
     * 기준 이미지를 원본 사진과 회원까지 조회하고 로그인 회원 소유인지 확인한다.
     */
    private BaseImage findOwnedBaseImage(Long memberId, Long baseImageId) {
        BaseImage baseImage = baseImageRepository.findByIdWithPhotoAndMember(baseImageId)
                .orElseThrow(() -> new CustomException(ErrorCode.BASE_IMAGE_NOT_FOUND));

        if (!Objects.equals(baseImage.getPhoto().getMember().getId(), memberId)) {
            throw new CustomException(ErrorCode.BASE_IMAGE_NOT_FOUND);
        }

        return baseImage;
    }

    /**
     * 착용 이미지 프롬프트에 필요한 신체 정보, 실측 치수와 착용 방식을 검증한다.
     */
    private void validateGenerationData(Member member, Product product) {
        if (member.getHeightCm() == null || member.getWeightKg() == null) {
            throw new CustomException(ErrorCode.WORN_IMAGE_BODY_INFO_REQUIRED);
        }

        Dimensions dimensions = product.getDimensions();
        if (dimensions == null
                || dimensions.getDepthIn() == null
                || dimensions.getWidthIn() == null
                || dimensions.getHeightIn() == null) {
            throw new CustomException(ErrorCode.PRODUCT_DIMENSIONS_REQUIRED);
        }

        if (product.getWearType() == null) {
            throw new CustomException(ErrorCode.PRODUCT_WEAR_TYPE_REQUIRED);
        }
    }

    /**
     * 실행 환경에서 선택한 이미지 생성 공급자를 착용 이미지 생성자 값으로 변환한다.
     */
    private Generator currentGenerator() {
        ImageGenerationProvider provider = imageGenerationProperties.provider();
        if (provider == null || provider == ImageGenerationProvider.NONE) {
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }

        return switch (provider) {
            case GEMINI -> Generator.GEMINI;
            case OPENAI -> Generator.OPENAI;
            case NONE -> throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        };
    }

    /**
     * 데이터베이스 저장에 실패한 생성 이미지를 고아 파일로 남기지 않도록 정리한다.
     */
    private void deleteQuietly(String storageKey) {
        try {
            imageStorage.delete(storageKey);
        } catch (Exception e) {
            log.error("사용하지 않는 착용 이미지 삭제에 실패했습니다. storageKey={}", storageKey, e);
        }
    }
}
