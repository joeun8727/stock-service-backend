package com.stocknews.api.client.financial;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record RawFinancialMetric(
        String period,
        LocalDate fiscalDate,
        // 기존 스냅샷
        BigDecimal roe,
        BigDecimal roa,
        BigDecimal roic,
        BigDecimal per,
        BigDecimal pbr,
        BigDecimal eps,
        BigDecimal debtRatio,
        BigDecimal interestCoverage,
        BigDecimal revenueGrowthYoy,
        BigDecimal operatingMargin,
        BigDecimal ocfToNi,
        // 신규 스냅샷
        BigDecimal psr,
        BigDecimal peg,
        BigDecimal grossMargin,
        BigDecimal fcfMargin,           // series.quarterly.fcfMargin 최신값
        BigDecimal marketCapMillions,   // marketCapitalization (백만 USD)
        // 추세 계산용 시계열 (TrendCalculator 전달 후 MetricTrend 저장)
        List<SeriesPoint> grossMarginSeries,
        List<SeriesPoint> roicSeries,
        List<SeriesPoint> operatingMarginSeries,
        List<SeriesPoint> fcfMarginSeries
) {}
