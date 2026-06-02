package com.stocknews.api.common.config;

// 캐시 이름 상수 — 문자열 하드코딩 금지 (07-caching.md)
public final class CacheKeys {

    private CacheKeys() {}

    public static final String STOCK_PROFILE          = "stockProfile";
    public static final String STOCK_METRICS          = "stockMetrics";
    public static final String STOCK_NEWS             = "stockNews";
    public static final String STOCK_SUMMARY          = "stockSummary";
    public static final String STOCK_SCORE_BREAKDOWN  = "stockScoreBreakdown";
    public static final String STOCK_SENTIMENT_TREND  = "stockSentimentTrend";
    public static final String SECTOR_RANKING         = "sectorRanking";
    public static final String SECTOR_LARGECAP        = "sectorLargecap";
    public static final String SECTOR_GROWTH          = "sectorGrowth";
    public static final String SECTOR_RULE_OF_40      = "sectorRuleOf40";
    public static final String SECTOR_VALUATION       = "sectorValuation";
    public static final String TOP_NEWS               = "topNews";
    public static final String MACRO                  = "macro";
}
