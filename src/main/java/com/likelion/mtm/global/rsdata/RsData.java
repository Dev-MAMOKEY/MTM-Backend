package com.likelion.mtm.global.rsdata;

import com.likelion.mtm.global.exception.ErrorCode;

import java.time.LocalDateTime;

public record RsData<T>(
        boolean success,
        T data,
        ErrorInfo error,
        LocalDateTime timestamp
) {
    public record ErrorInfo(
            String code,
            String message
    ) {
    }

    public static <T> RsData<T> success(T data) {
        return new RsData<>(true, data, null, LocalDateTime.now());
    }

    public static <T> RsData<T> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    public static <T> RsData<T> fail(ErrorCode errorCode, String message) {
        return new RsData<>(false, null, new ErrorInfo(errorCode.getCode(), message), LocalDateTime.now());
    }
}