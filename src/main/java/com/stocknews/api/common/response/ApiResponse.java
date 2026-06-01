package com.stocknews.api.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiResponse<T>(
        boolean success,
        T data,
        String disclaimer,
        ErrorInfo error,
        Instant timestamp
) {
    private static final String DISCLAIMER_TEXT =
            "본 정보는 투자 추천이나 자문이 아니며, 정보 제공만을 목적으로 합니다. " +
            "투자 결정과 그 책임은 전적으로 이용자 본인에게 있습니다.";

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, data, DISCLAIMER_TEXT, null, Instant.now());
    }

    public static <T> ApiResponse<T> failure(ErrorInfo error) {
        return new ApiResponse<>(false, null, DISCLAIMER_TEXT, error, Instant.now());
    }
}
