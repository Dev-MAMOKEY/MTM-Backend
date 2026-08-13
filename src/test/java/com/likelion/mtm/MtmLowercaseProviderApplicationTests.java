package com.likelion.mtm;

import com.likelion.mtm.domain.photo.controller.BaseImageController;
import com.likelion.mtm.domain.photo.controller.PhotoController;
import com.likelion.mtm.domain.photo.service.BaseImageService;
import com.likelion.mtm.infra.imagegen.ImageGenerationGateway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 소문자 NONE 공급자 설정에서도 이미지 생성 기능만 비활성화되는지 검증한다.
 */
@SpringBootTest(properties = "image-generation.provider=none")
class MtmLowercaseProviderApplicationTests {

    @Autowired
    private ApplicationContext applicationContext;

    /**
     * 소문자 none을 사용해도 기준 이미지 Bean 없이 기존 Photo 기능이 기동된다.
     */
    @Test
    void contextLoadsWithLowercaseNoneProvider() {
        assertThat(applicationContext.getBeansOfType(PhotoController.class)).hasSize(1);
        assertThat(applicationContext.getBeansOfType(BaseImageController.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(BaseImageService.class)).isEmpty();
        assertThat(applicationContext.getBeansOfType(ImageGenerationGateway.class)).isEmpty();
    }
}
