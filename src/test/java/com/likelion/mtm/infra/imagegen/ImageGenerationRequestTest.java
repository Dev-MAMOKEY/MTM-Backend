package com.likelion.mtm.infra.imagegen;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageGenerationRequestTest {

    @Test
    @DisplayName("이미지 생성 요청은 입력 목록과 바이트 배열을 외부 변경으로부터 보호한다")
    void protectRequestValues() {
        // given
        byte[] source = "image-data".getBytes();
        List<ImageInput> images = new ArrayList<>();
        images.add(new ImageInput(source, "image/jpeg"));

        // when
        ImageGenerationRequest request = new ImageGenerationRequest(images, "prompt");
        source[0] = 'X';
        images.clear();
        byte[] returned = request.images().get(0).data();
        returned[0] = 'Y';

        // then
        assertThat(request.images()).hasSize(1);
        assertThat(request.images().get(0).data())
                .isEqualTo("image-data".getBytes());
    }

    @Test
    @DisplayName("입력 이미지가 없거나 프롬프트가 비어 있으면 요청을 만들 수 없다")
    void rejectInvalidRequest() {
        assertThatThrownBy(() -> new ImageGenerationRequest(List.of(), "prompt"))
                .isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> new ImageGenerationRequest(
                List.of(new ImageInput("image".getBytes(), "image/png")),
                " "
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("생성 결과는 이미지가 아닌 MIME 타입을 거부한다")
    void rejectInvalidGeneratedImage() {
        assertThatThrownBy(() -> new GeneratedImage(
                "result".getBytes(),
                "application/json"
        )).isInstanceOf(IllegalArgumentException.class);
    }
}
