package com.stocknews.api.domain.sector;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocknews.api.domain.financial.FinancialMetric;
import com.stocknews.api.domain.financial.FinancialMetricRepository;
import com.stocknews.api.domain.financial.MetricTrend;
import com.stocknews.api.domain.financial.MetricTrendRepository;
import com.stocknews.api.domain.sector.SectorGroupConfig.SectorGroup;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 섹터 내 상대 평가(백분위 기반) 스코어링.
 * 절대값 비교 금지 — 반드시 같은 섹터 내 순위 백분위로 환산.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoringService {

    private static final String PERIOD      = "quarterly";
    private static final String LARGE_CAP   = "LARGE_CAP";
    private static final String GROWTH      = "GROWTH";
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    private final SectorRepository          sectorRepository;
    private final StockRepository           stockRepository;
    private final FinancialMetricRepository financialMetricRepository;
    private final MetricTrendRepository     metricTrendRepository;
    private final StockScoreRepository      stockScoreRepository;
    private final ObjectMapper              objectMapper;

    // ── 공개 API ──────────────────────────────────────────────────────────────

    @Transactional
    public void scoreAllSectors() {
        List<Sector> sectors = sectorRepository.findAll();
        for (Sector sector : sectors) {
            try {
                scoreSector(sector);
            } catch (Exception e) {
                log.error("섹터 스코어링 실패 — sector={}", sector.getCode(), e);
            }
        }
    }

    // ── 섹터 단위 스코어링 ────────────────────────────────────────────────────

    private void scoreSector(Sector sector) {
        SectorGroup group = SectorGroupConfig.of(sector.getCode());
        if (!SectorGroupConfig.growthTechCodes().contains(sector.getCode())
                && group == SectorGroup.TRADITIONAL) {
            // 매핑 확인 로그 (TRADITIONAL 기본값 적용)
        }

        List<Stock> allStocks = stockRepository
                .findBySectorIdAndMarketOrderByMarketCapDesc(sector.getId(), "US");

        if (allStocks.size() < ScoringWeights.MIN_SECTOR_SIZE_WARN) {
            log.warn("섹터 종목 수 신뢰도 경고 — sector={}, count={}", sector.getCode(), allStocks.size());
        }

        // 각 종목별 최신 지표 + 추세 수집
        List<StockWithData> dataList = new ArrayList<>();
        for (Stock stock : allStocks) {
            Optional<FinancialMetric> mOpt = financialMetricRepository
                    .findTopByStockIdAndPeriodOrderByFiscalDateDesc(stock.getId(), PERIOD);
            if (mOpt.isEmpty()) continue;

            FinancialMetric m = mOpt.get();

            // 하드 필터: 부채비율 초과 제외 (null은 통과)
            if (m.getDebtRatio() != null
                    && m.getDebtRatio().doubleValue() > ScoringWeights.DEBT_RATIO_MAX) {
                log.debug("부채비율 초과 제외 — ticker={}, debtRatio={}", stock.getTicker(), m.getDebtRatio());
                continue;
            }

            Map<String, BigDecimal> trends = loadTrends(stock.getId());
            dataList.add(new StockWithData(stock, m, trends));
        }

        if (dataList.isEmpty()) {
            log.info("스코어링 대상 없음 — sector={}", sector.getCode());
            return;
        }

        // ── 대형주 (시총 내림차순 Top 20) ────────────────────────────────────
        List<StockWithData> largeCapList = dataList.stream()
                .sorted(Comparator.comparing(d -> Optional.ofNullable(d.stock.getMarketCap())
                        .orElse(BigDecimal.ZERO), Comparator.reverseOrder()))
                .limit(20)
                .toList();

        saveLargeCapScores(sector, group, largeCapList);

        // ── 성장주 (시총 밴드 $10B~$500B, 대형주 중복 허용) ──────────────────
        // 섹터 종목이 20개 미만이면 전체가 대형주로 분류되어 성장주 풀이 비어버리는 문제 방지.
        // 대형주·성장주는 독립 기준 (중복 가능).
        BigDecimal minCap = BigDecimal.valueOf(ScoringWeights.GROWTH_MARKET_CAP_MIN);
        BigDecimal maxCap = BigDecimal.valueOf(ScoringWeights.GROWTH_MARKET_CAP_MAX);

        List<StockWithData> growthPool = dataList.stream()
                .filter(d -> {
                    BigDecimal cap = d.stock.getMarketCap();
                    if (cap == null) return false;
                    boolean inBand = cap.compareTo(minCap) >= 0 && cap.compareTo(maxCap) <= 0;
                    // GROWTH_TECH는 영업적자 허용
                    boolean profitOk = group == SectorGroup.GROWTH_TECH
                            || d.metric.getOperatingMargin() == null
                            || d.metric.getOperatingMargin().compareTo(BigDecimal.ZERO) >= 0;
                    return inBand && profitOk;
                })
                .toList();

        List<ScoredEntry> growthScored = scorePool(growthPool, group);
        saveGrowthScores(sector, group, growthScored);

        // is_growth_candidate 플래그 갱신
        Set<Long> growthIds = growthScored.stream()
                .limit(20)
                .map(e -> e.data.stock.getId()).collect(Collectors.toSet());

        for (Stock stock : allStocks) {
            boolean isCandidate = growthIds.contains(stock.getId());
            stock.updateGrowthCandidate(isCandidate);
            stockRepository.save(stock);
        }

        log.info("스코어링 완료 — sector={}, largeCap={}건, growth={}건",
                sector.getCode(), largeCapList.size(), Math.min(20, growthScored.size()));
    }

    // ── 대형주 점수 저장 (시총 순위 = 점수, 별도 가중치 없음) ─────────────────

    private void saveLargeCapScores(Sector sector, SectorGroup group,
                                     List<StockWithData> list) {
        for (int i = 0; i < list.size(); i++) {
            StockWithData d = list.get(i);
            int rank = i + 1;
            // 대형주는 시총 기준 순위 = 점수 (100 - (rank-1)*5 로 100~5점 부여)
            BigDecimal score = BigDecimal.valueOf(Math.max(0, 100 - (rank - 1) * 5));
            upsertScore(d.stock, sector, LARGE_CAP, group.name(), score, rank, Collections.emptyMap());
        }
    }

    // ── 성장주 풀 스코어링 ────────────────────────────────────────────────────

    private List<ScoredEntry> scorePool(List<StockWithData> pool, SectorGroup group) {
        if (pool.isEmpty()) return Collections.emptyList();

        // 지표별 섹터 내 백분위 계산
        Map<Long, Map<String, Double>> percentiles = computePercentiles(pool, group);

        List<ScoredEntry> result = new ArrayList<>();
        for (StockWithData d : pool) {
            Map<String, Double> pct = percentiles.getOrDefault(d.stock.getId(), Collections.emptyMap());
            double total = computeWeightedScore(pct, group);
            result.add(new ScoredEntry(d, BigDecimal.valueOf(total).setScale(2, RoundingMode.HALF_UP), pct));
        }

        result.sort(Comparator.comparing(ScoredEntry::score).reversed());
        return result;
    }

    // ── 백분위 계산 ───────────────────────────────────────────────────────────

    private Map<Long, Map<String, Double>> computePercentiles(List<StockWithData> pool,
                                                               SectorGroup group) {
        // 필요한 지표 목록
        List<String> metrics = group == SectorGroup.GROWTH_TECH
                ? List.of("ruleOf40", "grossMarginSlope", "roicSlope", "peg")
                : List.of("revenueGrowth", "operatingMarginSlope", "roicSlope", "psr");

        // 각 지표별 종목→값 수집
        Map<String, Map<Long, Double>> rawValues = new HashMap<>();
        for (String metric : metrics) {
            Map<Long, Double> vals = new HashMap<>();
            for (StockWithData d : pool) {
                Double v = extractMetricValue(d, metric);
                if (v != null) vals.put(d.stock.getId(), v);
            }
            rawValues.put(metric, vals);
        }

        // 지표별 LOWER_BETTER 여부
        Set<String> lowerBetter = Set.of("peg", "psr");

        // 백분위 환산
        Map<Long, Map<String, Double>> result = new HashMap<>();
        for (String metric : metrics) {
            Map<Long, Double> vals = rawValues.get(metric);
            Map<Long, Double> ranked = rankPercentile(vals, lowerBetter.contains(metric));
            for (Map.Entry<Long, Double> e : ranked.entrySet()) {
                result.computeIfAbsent(e.getKey(), k -> new HashMap<>())
                        .put(metric, e.getValue());
            }
        }
        return result;
    }

    /**
     * 순위 기반 백분위 (0~100). 동순위는 평균 순위.
     * lowerBetter=true이면 낮은 값이 높은 백분위.
     */
    private Map<Long, Double> rankPercentile(Map<Long, Double> vals, boolean lowerBetter) {
        if (vals.isEmpty()) return Collections.emptyMap();

        List<Map.Entry<Long, Double>> sorted = new ArrayList<>(vals.entrySet());
        sorted.sort(Comparator.comparingDouble(Map.Entry::getValue));

        // 동점 처리: 같은 값에 평균 순위 부여
        Map<Long, Double> result = new HashMap<>();
        int n = sorted.size();
        int i = 0;
        while (i < n) {
            int j = i;
            double v = sorted.get(i).getValue();
            while (j < n && sorted.get(j).getValue() == v) j++;
            double avgRank = (i + j - 1) / 2.0;  // 0-based
            double pct = n == 1 ? 50.0 : avgRank / (n - 1) * 100.0;
            if (lowerBetter) pct = 100.0 - pct;
            for (int k = i; k < j; k++) {
                result.put(sorted.get(k).getKey(), pct);
            }
            i = j;
        }
        return result;
    }

    // ── 지표값 추출 ───────────────────────────────────────────────────────────

    private Double extractMetricValue(StockWithData d, String metric) {
        FinancialMetric m = d.metric;
        return switch (metric) {
            case "ruleOf40" -> computeRuleOf40(m);
            case "grossMarginSlope" -> slopeValue(d.trends, "grossMargin");
            case "roicSlope"        -> slopeValue(d.trends, "roic");
            case "operatingMarginSlope" -> slopeValue(d.trends, "operatingMargin");
            case "revenueGrowth" -> m.getRevenueGrowthYoy() != null
                    ? m.getRevenueGrowthYoy().doubleValue() : null;
            case "peg" -> {
                // pegTTM 음수(적자)나 null → 중립값으로 대체 (페널티 없음)
                BigDecimal peg = m.getPeg();
                yield (peg == null || peg.compareTo(BigDecimal.ZERO) < 0) ? null : peg.doubleValue();
            }
            case "psr" -> m.getPsr() != null ? m.getPsr().doubleValue() : null;
            default -> null;
        };
    }

    private Double computeRuleOf40(FinancialMetric m) {
        if (m.getRevenueGrowthYoy() == null) return null;
        double rev = m.getRevenueGrowthYoy().doubleValue() * 100;  // % 단위
        double margin = 0;
        if (m.getFcfMargin() != null) {
            margin = m.getFcfMargin().doubleValue() * 100;
        } else if (m.getOperatingMargin() != null) {
            margin = m.getOperatingMargin().doubleValue() * 100;
        }
        return rev + margin;
    }

    private Double slopeValue(Map<String, BigDecimal> trends, String name) {
        BigDecimal v = trends.get(name);
        return v != null ? v.doubleValue() : null;
    }

    // ── 가중합 계산 ───────────────────────────────────────────────────────────

    private double computeWeightedScore(Map<String, Double> pct, SectorGroup group) {
        if (group == SectorGroup.GROWTH_TECH) {
            return neutral(pct, "ruleOf40")      * ScoringWeights.GT_RULE_OF_40
                 + neutral(pct, "grossMarginSlope") * ScoringWeights.GT_GROSS_MARGIN_SLOPE
                 + neutral(pct, "roicSlope")      * ScoringWeights.GT_ROIC_SLOPE
                 + neutral(pct, "peg")            * ScoringWeights.GT_PEG;
        } else {
            return neutral(pct, "revenueGrowth")      * ScoringWeights.TR_REVENUE_GROWTH
                 + neutral(pct, "operatingMarginSlope") * ScoringWeights.TR_OPERATING_MARGIN_SLOPE
                 + neutral(pct, "roicSlope")           * ScoringWeights.TR_ROIC_SLOPE
                 + neutral(pct, "psr")                 * ScoringWeights.TR_PSR;
        }
    }

    private double neutral(Map<String, Double> pct, String key) {
        return pct.getOrDefault(key, ScoringWeights.MISSING_PERCENTILE);
    }

    // ── StockScore 저장 ───────────────────────────────────────────────────────

    private void saveGrowthScores(Sector sector, SectorGroup group,
                                   List<ScoredEntry> scored) {
        int rank = 1;
        for (ScoredEntry e : scored) {
            if (rank > 20) break;
            upsertScore(e.data.stock, sector, GROWTH, group.name(),
                    e.score, rank++, e.percentileDetail);
        }
    }

    private void upsertScore(Stock stock, Sector sector, String screenType,
                              String groupName, BigDecimal score, int rank,
                              Map<String, Double> detail) {
        String detailJson = toJson(detail);
        stockScoreRepository.findByStockIdAndScreenType(stock.getId(), screenType)
                .ifPresentOrElse(
                        s -> s.update(score, rank, detailJson, groupName),
                        () -> stockScoreRepository.save(
                                new StockScore(stock, sector, screenType, groupName,
                                        score, rank, detailJson))
                );
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private Map<String, BigDecimal> loadTrends(Long stockId) {
        Map<String, BigDecimal> map = new HashMap<>();
        for (MetricTrend t : metricTrendRepository.findAllByStockId(stockId)) {
            map.put(t.getMetricName(), t.getSlope());
        }
        return map;
    }

    private String toJson(Map<String, Double> map) {
        if (map.isEmpty()) return null;
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            log.warn("score_detail JSON 직렬화 실패: {}", e.getMessage());
            return null;
        }
    }

    private record StockWithData(Stock stock, FinancialMetric metric,
                                  Map<String, BigDecimal> trends) {}

    private record ScoredEntry(StockWithData data, BigDecimal score,
                                Map<String, Double> percentileDetail) {}
}
