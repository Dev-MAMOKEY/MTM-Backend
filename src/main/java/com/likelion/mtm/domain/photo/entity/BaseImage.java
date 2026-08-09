package com.likelion.mtm.domain.photo.entity;

import com.likelion.mtm.global.common.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 기준 이미지 — 원본 사진을 정자세·흰 배경으로 변환한 것.
 * 원본 사진 하나당 하나만 만들어 재사용한다(1:1).
 * 배경 제거는 하지 않는다 — 흰옷·밝은 피부가 함께 지워져 인물에 구멍이 생기기 때문(ADR 0001).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "base_image",
        uniqueConstraints = @UniqueConstraint(name = "uk_base_image_photo_id", columnNames = "photo_id")
)
public class BaseImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "photo_id", nullable = false)
    private Photo photo;

    /** 다시 만들면 이 값이 교체된다 — 새 행을 만들지 않는다. */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Builder
    private BaseImage(Photo photo, String storageKey) {
        this.photo = photo;
        this.storageKey = storageKey;
    }

    /**
     * 최초 생성 시 사용하는 정적 팩토리 메서드.
     */
    public static BaseImage create(Photo photo, String storageKey) {
        return BaseImage.builder()
                .photo(photo)
                .storageKey(storageKey)
                .build();
    }

    /**
     * 기준 이미지를 다시 만들었을 때 저장소 키를 교체한다.
     * 이 시점에 연결된 착용 이미지들은 더 이상 유효하지 않으므로, 삭제는 서비스 계층에서
     * WornImageRepository.deleteAllByBaseImageId(...)로 함께 처리한다(엔티티 책임 밖).
     */
    public void replaceStorageKey(String newStorageKey) {
        this.storageKey = newStorageKey;
    }
}