package com.likelion.mtm.global.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

    // 공통
    BAD_REQUEST(HttpStatus.BAD_REQUEST, "BAD_REQUEST", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증되지 않은 사용자입니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "권한이 없는 사용자입니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 내부 오류가 발생했습니다."),

    // 회원가입 · 로그인
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS", "이미 가입된 이메일입니다."),
    // 미가입 이메일과 비밀번호 불일치를 같은 코드로 묶는다 — 어느 쪽이 틀렸는지 알려주면 계정 존재 여부가 새어 나간다
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", "이메일 또는 비밀번호가 일치하지 않습니다."),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "MEMBER_NOT_FOUND", "존재하지 않는 회원입니다."),

    // 원본 사진
    EMPTY_IMAGE_FILE(HttpStatus.BAD_REQUEST, "EMPTY_IMAGE_FILE", "이미지 파일이 비어 있습니다."),
    INVALID_IMAGE_FILE(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_FILE", "유효한 이미지 파일이 아닙니다."),
    IMAGE_STORAGE_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE_STORAGE_ERROR", "이미지 저장 중 오류가 발생했습니다."),
    FILE_SIZE_EXCEEDED(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_SIZE_EXCEEDED", "업로드 가능한 파일 크기를 초과했습니다."),

    // 기준 이미지
    PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "PHOTO_NOT_FOUND", "존재하지 않는 원본 사진입니다."),
    BODY_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "BODY_INFO_REQUIRED", "기준 이미지 생성에는 키와 몸무게가 필요합니다."),
    IMAGE_GENERATION_ERROR(HttpStatus.BAD_GATEWAY, "IMAGE_GENERATION_ERROR", "이미지 생성에 실패했습니다."),

    // 착용 이미지
    BASE_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "BASE_IMAGE_NOT_FOUND", "존재하지 않는 기준 이미지입니다."),
    PRODUCT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "존재하지 않는 제품입니다."),
    PRODUCT_CUT_NOT_FOUND(HttpStatus.NOT_FOUND, "PRODUCT_CUT_NOT_FOUND", "착용 이미지 생성에 사용할 제품 컷이 없습니다."),
    WORN_IMAGE_BODY_INFO_REQUIRED(HttpStatus.BAD_REQUEST, "WORN_IMAGE_BODY_INFO_REQUIRED", "착용 이미지 생성에는 키와 몸무게가 필요합니다."),
    PRODUCT_DIMENSIONS_REQUIRED(HttpStatus.BAD_REQUEST, "PRODUCT_DIMENSIONS_REQUIRED", "착용 이미지 생성에는 제품 실측 치수가 필요합니다."),
    PRODUCT_WEAR_TYPE_REQUIRED(HttpStatus.BAD_REQUEST, "PRODUCT_WEAR_TYPE_REQUIRED", "착용 이미지 생성에는 제품 착용 방식이 필요합니다."),
    WORN_IMAGE_ALREADY_EXISTS(HttpStatus.CONFLICT, "WORN_IMAGE_ALREADY_EXISTS", "해당 기준 이미지와 제품의 착용 이미지가 이미 존재합니다."),
    WORN_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "WORN_IMAGE_NOT_FOUND", "다시 만들 착용 이미지가 존재하지 않습니다."),

    // 토큰
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "토큰이 만료되었습니다."),
    TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "TOKEN_INVALID", "유효하지 않은 토큰입니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "리프레시 토큰이 만료되었습니다."),
    REFRESH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALID", "유효하지 않은 리프레시 토큰입니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }
}
