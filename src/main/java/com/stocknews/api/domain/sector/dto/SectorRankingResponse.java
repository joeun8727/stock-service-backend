package com.stocknews.api.domain.sector.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

// GET /api/v1/sectors/ranking 응답 DTO
public record SectorRankingResponse(
        Instant rankedAt,
        List<SectorRankItem> sectors
) {
    public record SectorRankItem(
            Long id,
            int rank,
            String code,
            String name,
            BigDecimal score,
            Highlights highlights
    ) {}

    public record Highlights(
            String newsVolumeChange,
            BigDecimal avgSentiment,
            BigDecimal avgRevenueGrowth
    ) {}
}
