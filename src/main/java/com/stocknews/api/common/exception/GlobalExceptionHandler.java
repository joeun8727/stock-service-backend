package com.stocknews.api.common.exception;

import com.stocknews.api.common.response.ApiResponse;
import com.stocknews.api.common.response.ErrorInfo;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("비즈니스 예외 발생: code={}, message={}", errorCode.getCode(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(ErrorInfo.of(errorCode.getCode(), e.getMessage())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse(ErrorCode.INVALID_INPUT.getMessage());
        log.warn("입력값 검증 실패: {}", message);
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.failure(ErrorInfo.of(ErrorCode.INVALID_INPUT.getCode(), message)));
    }

    // Resilience4j RateLimiter 초과
    @ExceptionHandler(RequestNotPermitted.class)
    public ResponseEntity<ApiResponse<Void>> handleRateLimitException(RequestNotPermitted e) {
        log.warn("외부 API Rate Limit 초과: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.EXTERNAL_API_RATE_LIMITED;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(ErrorInfo.of(errorCode.getCode(), errorCode.getMessage())));
    }

    // Resilience4j CircuitBreaker open
    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> handleCircuitOpenException(CallNotPermittedException e) {
        log.warn("외부 API 서킷 오픈: {}", e.getMessage());
        ErrorCode errorCode = ErrorCode.EXTERNAL_API_CIRCUIT_OPEN;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(ErrorInfo.of(errorCode.getCode(), errorCode.getMessage())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e) {
        log.error("예기치 않은 오류 발생", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        return ResponseEntity
                .status(errorCode.getHttpStatus())
                .body(ApiResponse.failure(ErrorInfo.of(errorCode.getCode(), errorCode.getMessage())));
    }
}
