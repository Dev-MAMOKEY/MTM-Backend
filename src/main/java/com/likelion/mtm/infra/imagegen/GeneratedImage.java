package com.likelion.mtm.infra.imagegen;

import java.util.Objects;

/**
 * 이미지 생성 모델이 반환한 이미지 바이트와 MIME 타입.
 */
public record GeneratedImage(
        byte[] data,
        String mimeType
) {

    public GeneratedImage {
        Objects.requireNonNull(data, "생성 이미지 데이터는 필수입니다.");
        Objects.requireNonNull(mimeType, "생성 이미지 MIME 타입은 필수입니다.");

        if (data.length == 0) {
            throw new IllegalArgumentException("생성 이미지 데이터는 비어 있을 수 없습니다.");
        }

        if (!mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("생성 결과는 이미지 MIME 타입이어야 합니다.");
        }

        data = data.clone();
    }

    /**
     * 외부에서 내부 바이트 배열을 변경하지 못하도록 복사본을 반환한다.
     */
    @Override
    public byte[] data() {
        return data.clone();
    }
}
