package com.likelion.mtm.domain.photo.controller;

import com.likelion.mtm.domain.photo.dto.BaseImageResponse;
import com.likelion.mtm.domain.photo.service.BaseImageService;
import com.likelion.mtm.global.rsdata.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이미지 생성 공급자가 선택된 환경에서 기준 이미지 생성 요청을 처리하는 API.
 */
@Tag(name = "기준 이미지", description = "기준 이미지 생성")
@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
@ConditionalOnExpression("!('${image-generation.provider:NONE}'.equalsIgnoreCase('NONE'))")
public class BaseImageController {

    private final BaseImageService baseImageService;

    /**
     * 로그인한 회원의 원본 사진으로 기준 이미지를 생성하거나 기존 결과를 반환한다.
     */
    @Operation(
            summary = "기준 이미지 생성",
            description = "원본 사진을 정자세와 불투명 흰 배경의 기준 이미지로 변환한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공 또는 기존 기준 이미지 반환"),
            @ApiResponse(responseCode = "400", description = "회원 신체 정보가 없음"),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 만료됨"),
            @ApiResponse(responseCode = "404", description = "원본 사진이 없거나 본인 소유가 아님"),
            @ApiResponse(responseCode = "502", description = "이미지 생성 모델 호출 실패")
    })
    @PostMapping("/{photoId}/base-image")
    public ResponseEntity<RsData<BaseImageResponse>> createBaseImage(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("photoId") Long photoId
    ) {
        return ResponseEntity.ok(
                RsData.success(baseImageService.create(memberId, photoId))
        );
    }
}
