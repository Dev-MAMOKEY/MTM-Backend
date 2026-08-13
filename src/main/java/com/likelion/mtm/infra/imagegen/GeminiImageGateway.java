package com.likelion.mtm.infra.imagegen;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.likelion.mtm.global.exception.CustomException;
import com.likelion.mtm.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Gen AI Java SDK를 사용하는 이미지 생성 게이트웨이 구현체.
 */
@Slf4j
@Component
@ConditionalOnProperty(prefix = "image-generation", name = "provider", havingValue = "GEMINI")
public class GeminiImageGateway implements ImageGenerationGateway {

    private final Client client;
    private final String model;

    /**
     * Gemini 클라이언트와 호출할 모델을 주입받는다.
     */
    public GeminiImageGateway(
            Client client,
            @Value("${gemini.model}") String model
    ) {
        this.client = client;
        this.model = model;
    }

    /**
     * 공급자 중립 요청을 Gemini 요청으로 변환하고 첫 번째 유효한 생성 이미지를 반환한다.
     */
    @Override
    public GeneratedImage generate(ImageGenerationRequest request) {
        try {
            Content content = toContent(request);
            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseModalities("TEXT", "IMAGE")
                    .candidateCount(1)
                    .build();

            GenerateContentResponse response = client.models.generateContent(model, content, config);

            return parseGeneratedImage(response);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("Gemini 이미지 생성에 실패했습니다.", e);
            throw new CustomException(ErrorCode.IMAGE_GENERATION_ERROR);
        }
    }

    /**
     * 프롬프트와 입력 이미지들을 Gemini Content로 변환한다.
     */
    Content toContent(ImageGenerationRequest request) {
        List<Part> parts = new ArrayList<>();
        parts.add(Part.fromText(request.prompt()));
        request.images().stream()
                .map(image -> Part.fromBytes(image.data(), normalizeMimeType(image.mimeType())))
                .forEach(parts::add);

        return Content.fromParts(parts.toArray(Part[]::new));
    }

    /**
     * 비표준 JPEG MIME 타입을 Gemini가 기대하는 표준 값으로 정규화한다.
     */
    private String normalizeMimeType(String mimeType) {
        return "image/jpg".equals(mimeType) ? "image/jpeg" : mimeType;
    }

    /**
     * Gemini 응답에서 첫 번째 이미지 inline data를 찾아 공급자 중립 결과로 변환한다.
     */
    GeneratedImage parseGeneratedImage(GenerateContentResponse response) {
        return response.candidates().orElse(List.of()).stream()
                .flatMap(candidate -> candidate.content().stream())
                .flatMap(content -> content.parts().orElse(List.of()).stream())
                .flatMap(part -> part.inlineData().stream())
                .filter(blob -> blob.data().filter(data -> data.length > 0).isPresent())
                .filter(blob -> blob.mimeType().filter(mimeType -> mimeType.startsWith("image/")).isPresent())
                .map(blob -> new GeneratedImage(blob.data().orElseThrow(), blob.mimeType().orElseThrow()))
                .findFirst()
                .orElseThrow(() -> new CustomException(ErrorCode.IMAGE_GENERATION_ERROR));
    }
}
