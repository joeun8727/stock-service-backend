package com.stocknews.api.domain.sector.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

// GET /api/v1/sectors/{sectorId}/valuation 응답 DTO
public record ValuationResponse(
        Long sectorId,
        String sectorCode,
        String sectorName,
        List<ValuationItem> stocks
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ValuationItem(
            String ticker,
            String companyName,
            BigDecimal marketCap,
            BigDecimal per,    // 주가수익비율
            BigDecimal pbr,    // 주가순자산비율
            BigDecimal psr,    // 주가매출비율
            BigDecimal peg,    // PEG (음수=적자)
            BigDecimal eps,    // 주당순이익
            BigDecimal roe,    // 수익성 맥락
            BigDecimal roic    // 수익성 맥락
    ) {}
}
