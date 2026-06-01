package com.stocknews.api.domain.sector;

import com.stocknews.api.client.financial.SeriesPoint;
import com.stocknews.api.domain.financial.MetricTrend;
import com.stocknews.api.domain.financial.MetricTrendRepository;
import com.stocknews.api.domain.stock.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.stream.Collectors;

/**
 * 시계열 선형회귀 기울기 계산 + MetricTrend 저장.
 * Finnhub series.quarterly는 최신→과거 순이므로 내부에서 역순 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TrendCalculator {

    private final MetricTrendRepository metricTrendRepository;

    private static final int    MAX_QUARTERS   = ScoringWeights.TREND_MAX_QUARTERS;
    private static final int    MIN_POINTS     = ScoringWeights.TREND_MIN_VALID_POINTS;
    private static final double WINSOR_RATIO   = ScoringWeights.TREND_WINSOR_RATIO;

    // ── 공개 API ──────────────────────────────────────────────────────────────

    @Transactional
    public void calculateAndStore(Stock stock, String metricName, List<SeriesPoint> series) {
        if (series == null || series.isEmpty()) return;

        calculateSlope(series).ifPresent(slope -> {
            int points = countValidPoints(series);
            BigDecimal slopeBd = slope.setScale(6, RoundingMode.HALF_UP);

            metricTrendRepository.findByStockIdAndMetricName(stock.getId(), metricName)
                    .ifPresentOrElse(
                            t -> t.update(slopeBd, points),
                            () -> metricTrendRepository.save(new MetricTrend(stock, metricName, slopeBd, points))
                    );
        });
    }

    /**
     * 순수 계산 유틸 — DB 저장 없이 기울기만 반환.
     * series: Finnhub 응답 그대로(최신→과거 순).
     */
    public Optional<BigDecimal> calculateSlope(List<SeriesPoint> series) {
        OptionalDouble slope = computeSlope(series);
        if (slope.isEmpty()) return Optional.empty();
        return Optional.of(BigDecimal.valueOf(slope.getAsDouble()).setScale(6, RoundingMode.HALF_UP));
    }

    // ── 내부 계산 ─────────────────────────────────────────────────────────────

    private OptionalDouble computeSlope(List<SeriesPoint> series) {
        // 최대 MAX_QUARTERS개 취득, 역순(오래된→최신)으로 변환
        int take = Math.min(series.size(), MAX_QUARTERS);
        List<SeriesPoint> window = new ArrayList<>(series.subList(0, take));
        Collections.reverse(window);  // 오래된→최신 순

        // 유효(non-null) 포인트 추출 (시간 인덱스 보존)
        record IndexedPoint(int idx, double y) {}
        List<IndexedPoint> pts = new ArrayList<>();
        for (int i = 0; i < window.size(); i++) {
            BigDecimal v = window.get(i).value();
            if (v != null) pts.add(new IndexedPoint(i, v.doubleValue()));
        }

        if (pts.size() < MIN_POINTS) return OptionalDouble.empty();

        // 윈저화 — 상하위 WINSOR_RATIO 클리핑
        List<Double> yVals = pts.stream().map(IndexedPoint::y)
                .sorted().collect(Collectors.toList());
        int clipN = Math.max(1, (int)(yVals.size() * WINSOR_RATIO));
        double yLow  = yVals.get(clipN - 1);
        double yHigh = yVals.get(yVals.size() - clipN);

        List<IndexedPoint> clipped = pts.stream()
                .map(p -> new IndexedPoint(p.idx(), Math.max(yLow, Math.min(yHigh, p.y()))))
                .collect(Collectors.toList());

        // 최소제곱 기울기
        double n    = clipped.size();
        double xBar = clipped.stream().mapToDouble(IndexedPoint::idx).average().orElse(0);
        double yBar = clipped.stream().mapToDouble(IndexedPoint::y).average().orElse(0);

        double num = 0, den = 0;
        for (IndexedPoint p : clipped) {
            double dx = p.idx() - xBar;
            num += dx * (p.y() - yBar);
            den += dx * dx;
        }

        if (den == 0) return OptionalDouble.empty();
        return OptionalDouble.of(num / den);
    }

    private int countValidPoints(List<SeriesPoint> series) {
        int take = Math.min(series.size(), MAX_QUARTERS);
        return (int) series.subList(0, take).stream()
                .filter(p -> p.value() != null).count();
    }
}
