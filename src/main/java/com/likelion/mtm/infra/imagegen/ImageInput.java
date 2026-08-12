package com.likelion.mtm.infra.imagegen;

import java.util.Objects;

/**
 * 이미지 생성 모델에 전달할 이미지 한 장의 바이트와 MIME 타입.
 */
public record ImageInput(
        byte[] data,
        String mimeType
) {

    public ImageInput {
        Objects.requireNonNull(data, "이미지 데이터는 필수입니다.");
        Objects.requireNonNull(mimeType, "이미지 MIME 타입은 필수입니다.");

        if (data.length == 0) {
            throw new IllegalArgumentException("이미지 데이터는 비어 있을 수 없습니다.");
        }

        if (!mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 MIME 타입만 사용할 수 있습니다.");
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
