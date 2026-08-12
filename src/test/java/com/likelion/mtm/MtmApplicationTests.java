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

@SpringBootTest
class MtmApplicationTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	void contextLoads() {
		assertThat(applicationContext.getBeansOfType(PhotoController.class)).hasSize(1);
		assertThat(applicationContext.getBeansOfType(BaseImageController.class)).isEmpty();
		assertThat(applicationContext.getBeansOfType(BaseImageService.class)).isEmpty();
		assertThat(applicationContext.getBeansOfType(ImageGenerationGateway.class)).isEmpty();
	}

}
