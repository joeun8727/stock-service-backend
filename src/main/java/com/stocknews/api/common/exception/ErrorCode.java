package com.stocknews.api.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "COMMON_001", "서버 내부 오류가 발생했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "COMMON_002", "입력값이 올바르지 않습니다."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "COMMON_003", "요청한 리소스를 찾을 수 없습니다."),

    // 종목
    STOCK_NOT_FOUND(HttpStatus.NOT_FOUND, "STOCK_001", "종목 정보를 찾을 수 없습니다."),

    // 종목 스코어
    SCORE_NOT_AVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "STOCK_002", "스코어 데이터가 아직 준비되지 않았습니다. 배치 실행 후 조회해 주세요."),

    // 섹터
    SECTOR_NOT_FOUND(HttpStatus.NOT_FOUND, "SECTOR_001", "섹터 정보를 찾을 수 없습니다."),
    SECTOR_RANKING_NOT_READY(HttpStatus.SERVICE_UNAVAILABLE, "SECTOR_002", "섹터 랭킹 데이터가 아직 준비되지 않았습니다."),

    // 외부 API
    EXTERNAL_API_ERROR(HttpStatus.BAD_GATEWAY, "EXT_001", "외부 API 호출에 실패했습니다."),
    EXTERNAL_API_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "EXT_002", "외부 API 호출 한도를 초과했습니다."),
    EXTERNAL_API_CIRCUIT_OPEN(HttpStatus.SERVICE_UNAVAILABLE, "EXT_003", "외부 API 서킷이 열려 있습니다. 잠시 후 다시 시도해 주세요.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
