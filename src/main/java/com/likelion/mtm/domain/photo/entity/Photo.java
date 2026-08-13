package com.likelion.mtm.domain.photo.entity;

import com.likelion.mtm.domain.member.entity.Member;
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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 원본 사진 — 회원이 업로드한 전신 사진. 아무 처리도 하지 않은 상태.
 * 한 회원의 사진들이 모여 사진첩이 된다. 개수 제한은 없다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "photo")
public class Photo extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소유 회원 — 사진첩은 본인 것만 조회된다. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    /** ImageStorage(로컬/S3)에서 파일을 찾는 키. */
    @Column(name = "storage_key", nullable = false, length = 500)
    private String storageKey;

    @Builder
    private Photo(Member member, String storageKey) {
        this.member = member;
        this.storageKey = storageKey;
    }

    /**
     * 업로드 시 사용하는 정적 팩토리 메서드.
     */
    public static Photo upload(Member member, String storageKey) {
        return Photo.builder()
                .member(member)
                .storageKey(storageKey)
                .build();
    }
}