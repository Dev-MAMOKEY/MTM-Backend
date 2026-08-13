package com.likelion.mtm.global.config;

import com.google.genai.Client;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gemini API 클라이언트를 운영 설정에 따라 구성한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "image-generation", name = "provider", havingValue = "GEMINI")
public class GeminiConfig {

    /**
     * 환경 변수로 주입받은 API key를 사용하는 Google Gen AI 클라이언트를 만든다.
     */
    @Bean
    public Client geminiClient(@Value("${gemini.api-key}") String apiKey) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY는 IMAGE_GENERATION_PROVIDER=GEMINI일 때 필수입니다."
            );
        }

        return Client.builder()
                .apiKey(apiKey)
                .build();
    }
}
