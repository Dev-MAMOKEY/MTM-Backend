package com.likelion.mtm.domain.worn.controller;

import com.likelion.mtm.domain.worn.dto.WornImageCreateRequest;
import com.likelion.mtm.domain.worn.dto.WornImageResponse;
import com.likelion.mtm.domain.worn.entity.Generator;
import com.likelion.mtm.domain.worn.service.WornImageService;
import com.likelion.mtm.global.rsdata.RsData;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 착용 이미지 생성 API가 인증 정보와 요청 값을 서비스에 전달하고 공통 응답으로 반환하는지 검증한다.
 */
@ExtendWith(MockitoExtension.class)
class WornImageControllerTest {

    @Mock
    private WornImageService wornImageService;

    @InjectMocks
    private WornImageController wornImageController;

    @Test
    @DisplayName("착용 이미지 생성 요청을 서비스에 전달하고 RsData 응답으로 반환한다")
    void createWornImage() {
        WornImageResponse serviceResponse = new WornImageResponse(
                40L,
                20L,
                30L,
                31L,
                "worn-image-url",
                Generator.GEMINI,
                LocalDateTime.of(2026, 8, 13, 1, 0)
        );
        when(wornImageService.create(1L, 20L, 30L)).thenReturn(serviceResponse);

        ResponseEntity<RsData<WornImageResponse>> response = wornImageController.createWornImage(
                1L,
                20L,
                new WornImageCreateRequest(30L)
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().data()).isEqualTo(serviceResponse);
        assertThat(response.getBody().error()).isNull();
        verify(wornImageService).create(1L, 20L, 30L);
    }
}
