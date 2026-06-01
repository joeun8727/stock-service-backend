package com.stocknews.api.domain.sector.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record SectorTrendResponse(
        Long sectorId,
        String sectorCode,
        String sectorName,
        List<DailyStats> dailyStats
) {
    public record DailyStats(
            LocalDate date,
            long newsCount,
            BigDecimal avgSentiment
    ) {}
}
