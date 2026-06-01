package com.stocknews.api.client.news;

import java.time.LocalDateTime;

public record RawNews(
        String headline,
        String source,
        String sourceUrl,
        LocalDateTime publishedAt,
        String snippet   // 요약 스니펫 (원문 전체 아님 — 저작권 준수)
) {}
