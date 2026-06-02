package com.stocknews.api.domain.stock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// GET /api/v1/stocks/{ticker}/sentiment-trend 응답 DTO
public record SentimentTrendResponse(
        String ticker,
        String companyName,
        int days,
        List<DailySentiment> trend
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record DailySentiment(
            LocalDate date,
            long newsCount,
            BigDecimal avgSentiment  // -1.0 ~ 1.0, LLM 처리 완료 기사만
    ) {}
}
