package com.likelion.mtm.domain.photo.controller;

import com.likelion.mtm.domain.photo.dto.PhotoResponse;
import com.likelion.mtm.domain.photo.service.PhotoService;
import com.likelion.mtm.global.rsdata.RsData;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 원본 사진 API — 로그인한 회원의 사진 업로드와 사진첩 조회를 담당한다.
 */
@Tag(name = "사진", description = "원본 사진 업로드 · 사진첩 조회")
@RestController
@RequestMapping("/api/v1/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    /**
     * 로그인한 회원의 원본 사진을 업로드한다.
     */
    @Operation(
            summary = "원본 사진 업로드",
            description = "로그인한 회원의 전신 사진을 저장하고 사진첩에 등록한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "업로드 성공"),
            @ApiResponse(responseCode = "400", description = "파일이 비었거나 이미지 파일이 아님"),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 만료됨"),
            @ApiResponse(responseCode = "500", description = "이미지 저장 실패")
    })
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<RsData<PhotoResponse>> uploadPhoto(
            @AuthenticationPrincipal Long memberId,
            @RequestPart("file") MultipartFile file
    ) {
        return ResponseEntity.ok(
                RsData.success(photoService.upload(memberId, file))
        );
    }

    /**
     * 로그인한 회원의 사진첩을 조회한다.
     */
    @Operation(
            summary = "내 사진첩 조회",
            description = "로그인한 회원이 업로드한 원본 사진 목록을 최신순으로 반환한다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공"),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 만료됨")
    })
    @GetMapping
    public ResponseEntity<RsData<List<PhotoResponse>>> getMyPhotos(
            @AuthenticationPrincipal Long memberId
    ) {
        return ResponseEntity.ok(
                RsData.success(photoService.getPhotos(memberId))
        );
    }

    /**
     * 로그인한 회원의 원본 사진을 삭제한다.
     */
    @Operation(
            summary = "원본 사진 삭제",
            description = "사진을 삭제한다. 그 사진으로 만든 기준 이미지와 착용 이미지도 함께 삭제되며 되돌릴 수 없다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공"),
            @ApiResponse(responseCode = "401", description = "토큰이 없거나 만료됨"),
            @ApiResponse(responseCode = "404", description = "존재하지 않거나 본인 소유가 아닌 사진")
    })
    @DeleteMapping("/{photoId}")
    public ResponseEntity<RsData<String>> deletePhoto(
            @AuthenticationPrincipal Long memberId,
            // 이름을 명시하지 않으면 IntelliJ 실행 시 500이 난다 (팀 컨벤션)
            @PathVariable("photoId") Long photoId
    ) {
        photoService.delete(memberId, photoId);
        return ResponseEntity.ok(RsData.success("사진이 삭제되었습니다."));
    }

}
