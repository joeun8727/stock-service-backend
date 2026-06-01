package com.stocknews.api.domain.news.dto;

import java.time.Instant;
import java.util.List;

// GET /api/v1/stocks/{ticker}/news 응답 DTO
public record NewsListResponse(
        String ticker,
        List<NewsItemResponse> news
) {
    public record NewsItemResponse(
            String headline,
            String source,
            String sourceUrl,
            Instant publishedAt,
            String summary,
            double sentiment,
            int importance,
            String relevance
    ) {}
}
