package com.stocknews.api.common.response;

public record ErrorInfo(
        String code,
        String message
) {
    public static ErrorInfo of(String code, String message) {
        return new ErrorInfo(code, message);
    }
}
