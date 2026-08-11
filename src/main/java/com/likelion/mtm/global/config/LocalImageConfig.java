package com.likelion.mtm.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(
        name = "image.storage.type",
        havingValue = "local",
        matchIfMissing = true
)
public class LocalImageConfig implements WebMvcConfigurer {

    private final Path rootPath;

    public LocalImageConfig(
            @Value("${image.storage.local.root-path}") String rootPath
    ) {
        this.rootPath = Path.of(rootPath).toAbsolutePath().normalize();
    }

    /**
     * 로컬 저장소의 이미지 파일을 /images/** URL로 제공한다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/images/**")
                .addResourceLocations(rootPath.toUri().toString());
    }
}