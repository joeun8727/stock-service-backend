package com.stocknews.api.domain.news.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// GET /api/v1/news/top 응답 DTO
public record TopNewsResponse(
        List<TopNewsItem> news,
        int count
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record TopNewsItem(
            String ticker,
            String companyName,
            String headline,
            String source,
            String sourceUrl,
            LocalDateTime publishedAt,
            String summary,
            BigDecimal sentiment,   // -1.0 ~ 1.0
            Integer importance,     // 0 ~ 100
            String relevance        // HIGH | MEDIUM | LOW
    ) {}
}
