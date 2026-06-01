package com.stocknews.api.domain.financial.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// GET /api/v1/stocks/{ticker}/financials 응답 DTO
public record FinancialMetricResponse(
        String ticker,
        String period,
        List<PeriodMetric> metrics
) {
    public record PeriodMetric(
            LocalDate fiscalDate,
            BigDecimal roe,
            BigDecimal roa,
            BigDecimal roic,
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal eps,
            BigDecimal debtRatio,
            BigDecimal interestCoverage,
            BigDecimal revenueGrowthYoy,
            BigDecimal operatingMargin,
            BigDecimal ocfToNi
    ) {}
}
