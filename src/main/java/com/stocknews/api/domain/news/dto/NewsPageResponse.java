package com.stocknews.api.domain.news.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// GET /api/v1/stocks/{ticker}/news 응답 DTO (페이지네이션 포함)
public record NewsPageResponse(
        String ticker,
        List<NewsItemResponse> news,
        long totalElements,
        int totalPages,
        int currentPage
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record NewsItemResponse(
            String headline,
            String source,
            String sourceUrl,
            LocalDateTime publishedAt,
            String summary,          // LLM 3줄 요약 (미처리 시 null)
            BigDecimal sentiment,    // -1.0 ~ 1.0 (미처리 시 null)
            Integer importance,      // 0 ~ 100 (미처리 시 null)
            String relevance         // HIGH | MEDIUM | LOW (미처리 시 null)
    ) {}
}
