package com.likelion.mtm.global.config;

import com.likelion.mtm.infra.imagegen.ImageGenerationProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * 이미지 생성 공급자 설정을 enum으로 바인딩해 잘못된 공급자 값을 시작 단계에서 거부한다.
 */
@Configuration
@EnableConfigurationProperties(ImageGenerationConfig.Properties.class)
public class ImageGenerationConfig {

    /**
     * 이미지 생성 공급자 설정 값.
     */
    @ConfigurationProperties(prefix = "image-generation")
    public record Properties(ImageGenerationProvider provider) {
    }
}
