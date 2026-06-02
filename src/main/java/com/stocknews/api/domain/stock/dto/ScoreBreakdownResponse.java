package com.stocknews.api.domain.stock.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

// GET /api/v1/stocks/{ticker}/score-breakdown 응답 DTO
public record ScoreBreakdownResponse(
        String ticker,
        String companyName,
        String sectorCode,
        String sectorName,
        List<ScoreEntry> scores
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ScoreEntry(
            String screenType,                // LARGE_CAP | GROWTH
            String sectorGroup,               // GROWTH_TECH | TRADITIONAL
            BigDecimal totalScore,            // 0~100
            int rankInSector,
            Map<String, Double> factorPercentiles, // 지표별 백분위 (LARGE_CAP은 빈 맵)
            LocalDateTime scoredAt
    ) {}
}
