package com.likelion.mtm.infra.storage;

import java.util.Objects;

/**
 * 이미지 저장소가 공급자와 무관하게 주고받는 이미지 바이너리와 MIME 타입.
 */
public record ImageData(
        byte[] data,
        String mimeType
) {

    /**
     * 이미지 데이터의 필수값과 형식을 검증하고 외부 변경을 막기 위해 배열을 복사한다.
     */
    public ImageData {
        Objects.requireNonNull(data, "이미지 데이터는 필수입니다.");
        Objects.requireNonNull(mimeType, "이미지 MIME 타입은 필수입니다.");

        if (data.length == 0) {
            throw new IllegalArgumentException("이미지 데이터는 비어 있을 수 없습니다.");
        }

        if (!mimeType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 MIME 타입만 저장할 수 있습니다.");
        }

        data = data.clone();
    }

    /**
     * 외부에서 내부 배열을 변경하지 못하도록 복사본을 반환한다.
     */
    @Override
    public byte[] data() {
        return data.clone();
    }
}
