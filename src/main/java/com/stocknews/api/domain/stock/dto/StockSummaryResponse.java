package com.stocknews.api.domain.stock.dto;

import java.math.BigDecimal;

// 종목 LLM 종합 요약 응답 DTO — GET /stocks/{ticker}/summary
public record StockSummaryResponse(
        String ticker,
        String companyName,
        String summaryComment,
        BigDecimal avgSentiment,
        int analyzedNewsCount
) {}
