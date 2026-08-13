package com.likelion.mtm.domain.worn.service;

import com.likelion.mtm.domain.photo.entity.BaseImage;
import com.likelion.mtm.domain.photo.repository.BaseImageRepository;
import com.likelion.mtm.domain.product.entity.Product;
import com.likelion.mtm.domain.product.entity.ProductCut;
import com.likelion.mtm.domain.worn.entity.Generator;
import com.likelion.mtm.domain.worn.entity.WornImage;
import com.likelion.mtm.domain.worn.repository.WornImageRepository;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 착용 이미지의 중복을 확인하고 최종 데이터베이스 저장을 짧은 트랜잭션으로 처리한다.
 */
@Service
@RequiredArgsConstructor
public class WornImagePersistenceService {

    private final BaseImageRepository baseImageRepository;
    private final WornImageRepository wornImageRepository;

    /**
     * 기준 이미지 행을 잠근 뒤 같은 제품의 착용 이미지가 없을 때만 새 저장소 키를 연결한다.
     */
    @Transactional
    public WornImage finalizeCreation(
            Long baseImageId,
            Product product,
            ProductCut productCut,
            String storageKey,
            Generator generator
    ) {
        BaseImage lockedBaseImage = baseImageRepository.findByIdForUpdate(baseImageId)
                .orElseThrow(() -> new CustomException(ErrorCode.BASE_IMAGE_NOT_FOUND));

        if (wornImageRepository.existsByBaseImageIdAndProductId(baseImageId, product.getId())) {
            throw new CustomException(ErrorCode.WORN_IMAGE_ALREADY_EXISTS);
        }

        return wornImageRepository.save(
                WornImage.create(lockedBaseImage, product, storageKey, generator, productCut)
        );
    }

    /**
     * 착용 이미지 행을 잠근 뒤 새 저장소 키로 교체한다. 새 행을 만들지 않고 기존 행을 갱신한다.
     * 교체 전 저장소 키를 함께 돌려줘 호출자가 트랜잭션 밖에서 옛 파일을 정리할 수 있게 한다.
     */
    @Transactional
    public RegenerationResult finalizeRegeneration(
            Long baseImageId,
            Long productId,
            String newStorageKey,
            Generator generator,
            ProductCut productCut
    ) {
        WornImage lockedWornImage = wornImageRepository
                .findByBaseImageIdAndProductIdForUpdate(baseImageId, productId)
                .orElseThrow(() -> new CustomException(ErrorCode.WORN_IMAGE_NOT_FOUND));

        String previousStorageKey = lockedWornImage.getStorageKey();
        lockedWornImage.replaceImage(newStorageKey, generator, productCut);

        return new RegenerationResult(lockedWornImage, previousStorageKey);
    }

    /**
     * 재생성 확정 결과와 정리 대상인 교체 전 저장소 키를 함께 전달한다.
     */
    public record RegenerationResult(
            WornImage wornImage,
            String previousStorageKey
    ) {
    }
}
