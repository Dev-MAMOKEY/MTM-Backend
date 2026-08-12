package com.likelion.mtm.global.config;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAI 이미지 API 클라이언트를 공급자 설정에 따라 구성한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "image-generation", name = "provider", havingValue = "OPENAI")
public class OpenAiConfig {

    /**
     * 환경 변수로 주입받은 API key를 사용하는 OpenAI 클라이언트를 만든다.
     */
    @Bean
    public OpenAIClient openAiClient(@Value("${openai.api-key}") String apiKey) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException(
                    "OPENAI_API_KEY는 IMAGE_GENERATION_PROVIDER=OPENAI일 때 필수입니다."
            );
        }

        return OpenAIOkHttpClient.builder()
                .apiKey(apiKey)
                .build();
    }
}
