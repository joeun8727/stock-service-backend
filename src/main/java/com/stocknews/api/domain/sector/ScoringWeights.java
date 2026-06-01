package com.stocknews.api.domain.sector;

/**
 * 섹터 그룹별 가중치 상수.
 * 모든 가중치 합 = 1.0.
 */
public final class ScoringWeights {

    private ScoringWeights() {}

    // ── 성장 테크 그룹 (GROWTH_TECH) ──────────────────────────────────────────
    public static final double GT_RULE_OF_40          = 0.35;
    public static final double GT_GROSS_MARGIN_SLOPE  = 0.25;
    public static final double GT_ROIC_SLOPE          = 0.20;
    public static final double GT_PEG                 = 0.20; // LOWER_BETTER

    // ── 전통/수익성 그룹 (TRADITIONAL) ───────────────────────────────────────
    public static final double TR_REVENUE_GROWTH          = 0.40;
    public static final double TR_OPERATING_MARGIN_SLOPE  = 0.25;
    public static final double TR_ROIC_SLOPE              = 0.20;
    public static final double TR_PSR                     = 0.15; // LOWER_BETTER

    // ── 하드 필터 임계값 ─────────────────────────────────────────────────────
    /** 부채비율(D/E) 초과 시 제외 */
    public static final double DEBT_RATIO_MAX             = 2.0;

    // ── 성장주 시총 밴드 (USD) ───────────────────────────────────────────────
    /** 성장주 시총 하한 ($10B) */
    public static final long GROWTH_MARKET_CAP_MIN        = 10_000_000_000L;
    /** 성장주 시총 상한 ($500B) */
    public static final long GROWTH_MARKET_CAP_MAX        = 500_000_000_000L;

    // ── TrendCalculator 파라미터 ─────────────────────────────────────────────
    public static final int  TREND_MAX_QUARTERS            = 8;
    public static final int  TREND_MIN_VALID_POINTS        = 3;
    /** 윈저화 상하위 클리핑 비율 */
    public static final double TREND_WINSOR_RATIO          = 0.10;

    // ── 스코어링 ─────────────────────────────────────────────────────────────
    /** 결측 지표 대체 백분위 (중립값) */
    public static final double MISSING_PERCENTILE          = 50.0;
    /** 섹터 종목 수 신뢰도 경고 임계값 */
    public static final int  MIN_SECTOR_SIZE_WARN          = 10;

    // ── 종목 마스터 수집 ─────────────────────────────────────────────────────
    /** 섹터별 시총 상위 후보 수 */
    public static final int  STOCK_MASTER_TOP_N_PER_SECTOR = 100;
}
