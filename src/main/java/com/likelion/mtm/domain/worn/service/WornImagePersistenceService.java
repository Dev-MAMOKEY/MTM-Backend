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
}
