package com.likelion.mtm.infra.imagegen;

import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Candidate;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * 네트워크 호출 없이 Gemini SDK 요청 변환과 응답 파싱을 검증한다.
 */
class GeminiImageGatewayTest {

    private final GeminiImageGateway gateway =
            new GeminiImageGateway(mock(Client.class), "test-model");

    @Test
    @DisplayName("공급자 중립 요청을 프롬프트와 inline image Part로 변환한다")
    void convertRequest() {
        ImageGenerationRequest request = new ImageGenerationRequest(
                List.of(new ImageInput("source".getBytes(), "image/jpeg")),
                "make a base image"
        );

        Content content = gateway.toContent(request);
        List<Part> parts = content.parts().orElseThrow();

        assertThat(parts).hasSize(2);
        assertThat(parts.get(0).text()).contains("make a base image");
        assertThat(parts.get(1).inlineData().orElseThrow().data().orElseThrow())
                .isEqualTo("source".getBytes());
        assertThat(parts.get(1).inlineData().orElseThrow().mimeType())
                .contains("image/jpeg");
    }

    @Test
    @DisplayName("입력 이미지 MIME 타입을 Gemini 표준 값으로 전달한다")
    void normalizeInputMimeTypes() {
        ImageGenerationRequest request = new ImageGenerationRequest(
                List.of(
                        new ImageInput("jpg".getBytes(), "image/jpg"),
                        new ImageInput("jpeg".getBytes(), "image/jpeg"),
                        new ImageInput("png".getBytes(), "image/png"),
                        new ImageInput("webp".getBytes(), "image/webp")
                ),
                "prompt"
        );

        List<Part> parts = gateway.toContent(request).parts().orElseThrow();

        assertThat(parts.subList(1, parts.size()))
                .extracting(part -> part.inlineData().orElseThrow().mimeType().orElseThrow())
                .containsExactly("image/jpeg", "image/jpeg", "image/png", "image/webp");
    }

    @Test
    @DisplayName("Gemini 응답의 image inline data를 생성 이미지로 변환한다")
    void parseImageResponse() {
        GenerateContentResponse response = responseWithParts(
                Part.fromText("completed"),
                Part.builder()
                        .inlineData(Blob.builder()
                                .data("generated".getBytes())
                                .mimeType("image/png")
                                .build())
                        .build()
        );

        GeneratedImage generatedImage = gateway.parseGeneratedImage(response);

        assertThat(generatedImage.data()).isEqualTo("generated".getBytes());
        assertThat(generatedImage.mimeType()).isEqualTo("image/png");
    }

    @Test
    @DisplayName("Gemini 응답에 이미지가 없으면 명시적인 생성 오류가 발생한다")
    void rejectTextOnlyResponse() {
        GenerateContentResponse response = responseWithParts(Part.fromText("no image"));

        assertThatThrownBy(() -> gateway.parseGeneratedImage(response))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.IMAGE_GENERATION_ERROR);
    }

    private GenerateContentResponse responseWithParts(Part... parts) {
        Content content = Content.builder()
                .parts(List.of(parts))
                .build();
        Candidate candidate = Candidate.builder()
                .content(content)
                .build();
        return GenerateContentResponse.builder()
                .candidates(List.of(candidate))
                .build();
    }
}
