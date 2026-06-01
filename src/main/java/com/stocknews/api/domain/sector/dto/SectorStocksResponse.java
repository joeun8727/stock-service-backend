package com.stocknews.api.domain.sector.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;

public record SectorStocksResponse(
        Long sectorId,
        String sectorCode,
        String sectorName,
        String type,
        List<StockScreenResult> stocks
) {
    public record StockScreenResult(
            int rank,
            String ticker,
            String companyName,
            BigDecimal marketCap,
            @JsonInclude(JsonInclude.Include.NON_NULL) MetricsSnapshot metrics,
            @JsonInclude(JsonInclude.Include.NON_NULL) BigDecimal growthScore
    ) {}

    public record MetricsSnapshot(
            BigDecimal roe,
            BigDecimal roa,
            BigDecimal roic,
            BigDecimal per,
            BigDecimal pbr,
            BigDecimal revenueGrowthYoy,
            BigDecimal operatingMargin,
            // 신규 (V4)
            BigDecimal psr,
            BigDecimal peg,
            BigDecimal grossMargin,
            BigDecimal fcfMargin
    ) {}
}
