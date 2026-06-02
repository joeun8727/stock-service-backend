package com.stocknews.api.domain.sector.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

// GET /api/v1/sectors/{sectorId}/rule-of-40 응답 DTO
public record RuleOf40Response(
        Long sectorId,
        String sectorCode,
        String sectorName,
        String sectorGroup,          // GROWTH_TECH | TRADITIONAL
        List<RuleOf40Item> stocks
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record RuleOf40Item(
            int rank,
            String ticker,
            String companyName,
            BigDecimal marketCap,
            BigDecimal revenueGrowthPct,      // % 단위 (예: 45.2)
            BigDecimal fcfMarginPct,          // % 단위, null 가능
            BigDecimal operatingMarginPct,    // % 단위, fcfMargin 없을 때 대체
            BigDecimal ruleOf40Score          // revenueGrowthPct + max(fcfMarginPct, operatingMarginPct)
    ) {}
}
