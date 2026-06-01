package com.stocknews.api.domain.stock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StockProfileResponse(
        String ticker,
        String companyName,
        String sector,
        String industry,
        BigDecimal marketCap,
        String exchange,
        String website,
        Integer employeeCount,
        LocalDate ipoDate,
        @JsonInclude(JsonInclude.Include.NON_NULL) LatestMetrics latestMetrics
) {
    // API 계약 StockMetrics (루트 CLAUDE.md) — 배치 수집 전이면 null
    public record LatestMetrics(
            BigDecimal roe,
            BigDecimal roa,
            BigDecimal roic,
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal eps,
            BigDecimal debtRatio,
            BigDecimal revenueGrowthYoy,
            BigDecimal operatingMargin
    ) {}
}
