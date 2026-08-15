package com.likelion.mtm.domain.worn.repository;

import com.likelion.mtm.domain.worn.entity.WornImage;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 착용 이미지의 데이터베이스 접근을 담당하는 리포지토리.
 */
public interface WornImageRepository extends JpaRepository<WornImage, Long> {

    /**
     * 기준 이미지와 제품 조합에 이미 착용 이미지가 존재하는지 확인한다.
     * 저장 확정 시 잠금 이후의 중복 방지 가드로 쓰인다.
     */
    boolean existsByBaseImageIdAndProductId(Long baseImageId, Long productId);

    /**
     * 기준 이미지와 제품 조합으로 저장된 착용 이미지를 조회한다.
     * "이미 본 제품은 즉시 다시 뜬다" — 있으면 새로 생성하지 않고 이걸 돌려준다.
     */
    Optional<WornImage> findByBaseImageIdAndProductId(Long baseImageId, Long productId);

    /**
     * 재생성 저장 확정 동안 착용 이미지 행을 비관적 쓰기 잠금으로 조회한다.
     * "다시 만들기"는 새 행이 아니라 이 행의 저장소 키를 교체한다.
     *
     * @param baseImageId 기준 이미지 식별자
     * @param productId   제품 식별자
     * @return 잠긴 착용 이미지
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from WornImage w where w.baseImage.id = :baseImageId and w.product.id = :productId")
    Optional<WornImage> findByBaseImageIdAndProductIdForUpdate(
            @Param("baseImageId") Long baseImageId,
            @Param("productId") Long productId
    );

    /**
     * 기준 이미지에 딸린 착용 이미지를 모두 삭제하고, 정리된 것들을 돌려준다.
     * 기준 이미지를 다시 만들면 옛 기준 이미지 위의 착용 이미지는 새 얼굴과 맞지 않으므로 함께 지운다.
     * 반환된 목록의 저장소 키로 호출자가 트랜잭션 밖에서 파일을 정리한다.
     *
     * @param baseImageId 기준 이미지 식별자
     * @return 삭제된 착용 이미지 목록
     */
    List<WornImage> deleteAllByBaseImageId(Long baseImageId);
}
