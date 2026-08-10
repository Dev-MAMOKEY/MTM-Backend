package com.likelion.mtm.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * 개발 환경에서 로컬에 저장된 이미지를 HTTP로 조회할 수 있도록 설정한다.
 */
@Configuration
@Profile("local")
public class LocalImageConfig implements WebMvcConfigurer {

    private final String rootPath;

    public LocalImageConfig(
            @Value("${image.storage.local.root-path}") String rootPath
    ) {
        this.rootPath = rootPath;
    }

    /**
     * /images/** 요청을 로컬 이미지 저장 경로와 연결한다.
     */
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Paths.get(rootPath)
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        registry.addResourceHandler("/images/**")
                .addResourceLocations(location);
    }
}