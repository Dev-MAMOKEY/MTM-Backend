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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
     * 이미 같은 조합의 착용 이미지가 있으면 새로 생성하지 않고 저장된 것을 즉시 반환한다.
     */
    @Operation(
            summary = "착용 이미지 생성",
            description = "기준 이미지와 선택한 제품의 정면 제품 컷을 사용해 착용 이미지를 생성하고 저장한다. "
                    + "이미 같은 조합(기준 이미지, 제품)의 착용 이미지가 있으면 새로 생성하지 않고 저장된 것을 즉시 반환한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "생성 성공 또는 이미 생성된 착용 이미지를 즉시 반환"),
            @ApiResponse(responseCode = "400", description = "신체 정보, 제품 실측 치수 또는 착용 방식이 없음"),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 만료됨"),
            @ApiResponse(responseCode = "404", description = "기준 이미지, 제품 또는 제품 컷이 없거나 기준 이미지가 본인 소유가 아님"),
            @ApiResponse(responseCode = "409", description = "동시 요청으로 동일 조합이 먼저 저장되어 충돌함(드묾)"),
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

    /**
     * 로그인 회원의 (기준 이미지, 제품) 조합으로 이미 만든 착용 이미지를 새로 생성해 교체한다.
     * 새 행을 만들지 않고 기존 착용 이미지를 교체하며, 대상이 없으면 실패한다.
     */
    @Operation(
            summary = "착용 이미지 재생성",
            description = "이미 만든 (기준 이미지, 제품) 조합의 착용 이미지를 새로 생성해 기존 것을 교체한다. "
                    + "새 행을 만들지 않고 저장된 착용 이미지를 교체하며, 다시 만들 대상이 없으면 실패한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "재생성 성공, 교체된 착용 이미지를 반환"),
            @ApiResponse(responseCode = "400", description = "신체 정보, 제품 실측 치수 또는 착용 방식이 없음"),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 만료됨"),
            @ApiResponse(responseCode = "404", description = "기준 이미지·제품·제품 컷이 없거나, 다시 만들 착용 이미지가 없거나, "
                    + "기준 이미지가 본인 소유가 아님"),
            @ApiResponse(responseCode = "500", description = "이미지 저장 실패"),
            @ApiResponse(responseCode = "502", description = "이미지 생성 모델 호출 실패")
    })
    @PostMapping("/{baseImageId}/worn-images/regenerate")
    public ResponseEntity<RsData<WornImageResponse>> regenerateWornImage(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("baseImageId") Long baseImageId,
            @Valid @RequestBody WornImageCreateRequest request
    ) {
        return ResponseEntity.ok(
                RsData.success(
                        wornImageService.regenerate(memberId, baseImageId, request.productId())
                )
        );
    }

    /**
     * 로그인 회원 소유의 기준 이미지에 딸린 착용 이미지 목록을 조회한다.
     * 착용 화면의 "착용 이미지 N장" 표시와 재생성 경고에 쓰인다.
     */
    @Operation(
            summary = "기준 이미지별 착용 이미지 목록 조회",
            description = "기준 이미지에 딸린 착용 이미지 목록을 최신순으로 반환한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 만료됨"),
            @ApiResponse(responseCode = "404", description = "기준 이미지가 없거나 본인 소유가 아님")
    })
    @GetMapping("/{baseImageId}/worn-images")
    public ResponseEntity<RsData<List<WornImageResponse>>> getWornImages(
            @AuthenticationPrincipal Long memberId,
            @PathVariable("baseImageId") Long baseImageId
    ) {
        return ResponseEntity.ok(
                RsData.success(wornImageService.getWornImages(memberId, baseImageId))
        );
    }
}
