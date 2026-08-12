package com.likelion.mtm.domain.worn.controller;

import com.likelion.mtm.domain.worn.dto.WornImageCreateRequest;
import com.likelion.mtm.domain.worn.dto.WornImageResponse;
import com.likelion.mtm.domain.worn.service.WornImageService;
import com.likelion.mtm.global.rsdata.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이미지 생성 공급자가 선택된 환경에서 착용 이미지 생성 요청을 처리하는 API.
 */
@Tag(name = "착용 이미지", description = "착용 이미지 생성")
@RestController
@RequestMapping("/api/v1/base-images")
@RequiredArgsConstructor
@ConditionalOnExpression("!('${image-generation.provider:NONE}'.equalsIgnoreCase('NONE'))")
public class WornImageController {

    private final WornImageService wornImageService;

    /**
     * 로그인 회원의 기준 이미지와 선택한 제품으로 착용 이미지를 생성한다.
     */
    @Operation(
            summary = "착용 이미지 생성",
            description = "기준 이미지와 선택한 제품의 정면 제품 컷을 사용해 착용 이미지를 생성하고 저장한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공"),
            @ApiResponse(responseCode = "400", description = "신체 정보, 제품 실측 치수 또는 착용 방식이 없음"),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 만료됨"),
            @ApiResponse(responseCode = "404", description = "기준 이미지, 제품 또는 제품 컷이 없거나 기준 이미지가 본인 소유가 아님"),
            @ApiResponse(responseCode = "409", description = "동일한 기준 이미지와 제품의 착용 이미지가 이미 존재함"),
            @ApiResponse(responseCode = "500", description = "이미지 저장 실패"),
            @ApiResponse(responseCode = "502", description = "이미지 생성 모델 호출 실패")
    })
    @PostMapping("/{baseImageId}/worn-images")
    public ResponseEntity<RsData<WornImageResponse>> createWornImage(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("baseImageId") Long baseImageId,
            @Valid @RequestBody WornImageCreateRequest request
    ) {
        return ResponseEntity.ok(
                RsData.success(
                        wornImageService.create(memberId, baseImageId, request.productId())
                )
        );
    }
}
