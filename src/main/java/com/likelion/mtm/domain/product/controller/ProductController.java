package com.likelion.mtm.domain.product.controller;

import com.likelion.mtm.domain.product.dto.ProductResponse;
import com.likelion.mtm.domain.product.service.ProductService;
import com.likelion.mtm.global.rsdata.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 제품 API — 회원이 착용해 볼 MCM 제품을 고르는 화면이 쓴다 */
@Tag(name = "제품", description = "제품 목록 조회")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @Operation(
            summary = "제품 목록 조회",
            description = "적재된 MCM 제품 전체를 대표 이미지와 함께 돌려준다. "
                    + "대표 이미지 URL은 유효기간 30분의 Presigned URL이라 오래 캐시하면 안 된다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 만료됨")
    })
    @GetMapping
    public ResponseEntity<RsData<List<ProductResponse>>> getProducts() {
        return ResponseEntity.ok(RsData.success(productService.getProducts()));
    }
}
