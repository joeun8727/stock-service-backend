package com.stocknews.api.domain.sector;

import com.stocknews.api.client.macro.RawMacroData;
import com.stocknews.api.common.config.CacheKeys;
import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.domain.financial.FinancialMetric;
import com.stocknews.api.domain.financial.FinancialMetricRepository;
import com.stocknews.api.domain.news.NewsArticleRepository;
import com.stocknews.api.domain.sector.dto.SectorRankingResponse;
import com.stocknews.api.domain.sector.dto.SectorRankingResponse.Highlights;
import com.stocknews.api.domain.sector.dto.SectorRankingResponse.SectorRankItem;
import com.stocknews.api.domain.sector.dto.SectorTrendResponse;
import com.stocknews.api.domain.sector.dto.SectorTrendResponse.DailyStats;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SectorRankingService {

    // ──────────────────────────────────────────────
    // 가중치 (04-sector-ranking.md)
    // ──────────────────────────────────────────────
    private static final double W_NEWS_VOLUME    = 0.25;
    private static final double W_SENTIMENT      = 0.20;
    private static final double W_REVENUE_GROWTH = 0.25;
    private static final double W_MACRO          = 0.20;
    private static final double W_MOMENTUM       = 0.10;

    // 매출성장률 정규화 범위 (스크리닝과 동일)
    private static final BigDecimal REVENUE_MIN   = new BigDecimal("-20");   // -20%
    private static final BigDecimal REVENUE_MAX   = new BigDecimal("100");  // 100% (이상치 클램핑)
    private static final BigDecimal REVENUE_RANGE = REVENUE_MAX.subtract(REVENUE_MIN);
    private static final BigDecimal HUNDRED       = BigDecimal.valueOf(100);

    // DFF 기준 금리 환경 임계값
    private static final BigDecimal RATE_LOW_THRESHOLD  = new BigDecimal("3.0");
    private static final BigDecimal RATE_HIGH_THRESHOLD = new BigDecimal("5.5");

    private static final String FRED_DFF_SERIES = "DFF";  // Federal Funds Rate

    // ──────────────────────────────────────────────
    // 섹터별 금리 환경 민감도 점수 표 [LOW_RATE, MEDIUM_RATE, HIGH_RATE]
    // 낮은 금리 → 성장주 섹터 유리, 높은 금리 → 금융 섹터 유리
    // ──────────────────────────────────────────────
    private static final Map<String, double[]> MACRO_SCORE_TABLE = Map.ofEntries(
            Map.entry("SEMICONDUCTOR",   new double[]{90, 55, 25}),
            Map.entry("AEROSPACE",       new double[]{85, 50, 20}),
            Map.entry("AI_SOFTWARE",     new double[]{90, 60, 30}),
            Map.entry("EV_BATTERY",      new double[]{80, 45, 20}),
            Map.entry("HEALTHCARE_BIO",  new double[]{65, 55, 40}),
            Map.entry("ENERGY",          new double[]{50, 55, 60}),
            Map.entry("FINANCE",         new double[]{30, 55, 85}),
            Map.entry("CONSUMER_GOODS",  new double[]{70, 50, 35}),
            Map.entry("ROBOTICS",        new double[]{85, 55, 25}),
            Map.entry("CYBERSECURITY",   new double[]{80, 65, 50})
    );
    private static final double[] DEFAULT_MACRO_SCORE = {50, 50, 50};

    private enum MacroRegime { LOW_RATE, MEDIUM_RATE, HIGH_RATE }

    private final SectorRepository sectorRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final FinancialMetricRepository financialMetricRepository;
    private final StockRepository stockRepository;
    private final MacroDataService macroDataService;

    // ──────────────────────────────────────────────
    // GET /api/v1/sectors/ranking (캐시 12시간)
    // ──────────────────────────────────────────────

    @Cacheable(value = CacheKeys.SECTOR_RANKING, key = "#limit")
    public SectorRankingResponse getRankings(int limit) {
        List<Sector> ranked = sectorRepository.findAllByOrderByLatestRankAsc().stream()
                .filter(s -> s.getLatestRank() != null)
                .limit(limit)
                .toList();

        if (ranked.isEmpty()) {
            throw new BusinessException(ErrorCode.SECTOR_RANKING_NOT_READY);
        }

        LocalDateTime now = LocalDateTime.now();
        List<SectorRankItem> items = ranked.stream()
                .map(sector -> toRankItem(sector, now))
                .toList();

        // rankedAt은 가장 최근 갱신 시각
        LocalDateTime rankedAt = ranked.stream()
                .map(Sector::getRankedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(now);

        return new SectorRankingResponse(rankedAt.atZone(ZoneId.of("UTC")).toInstant(), items);
    }

    // ──────────────────────────────────────────────
    // GET /api/v1/sectors/{sectorId}/trend
    // ──────────────────────────────────────────────

    public SectorTrendResponse getSectorTrend(Long sectorId) {
        Sector sector = sectorRepository.findById(sectorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTOR_NOT_FOUND));

        LocalDateTime from = LocalDateTime.now().minusDays(14);
        List<NewsArticleRepository.DailyNewsStat> raw =
                newsArticleRepository.dailyStatsBySector(sectorId, from);

        List<DailyStats> stats = raw.stream()
                .map(r -> new DailyStats(
                        r.getStatDate(),           // LocalDate — toLocalDate() 불필요
                        r.getNewsCount(),
                        r.getAvgSentiment() != null
                                ? r.getAvgSentiment().setScale(4, RoundingMode.HALF_UP)
                                : null
                ))
                .toList();

        return new SectorTrendResponse(sectorId, sector.getCode(), sector.getName(), stats);
    }

    // ──────────────────────────────────────────────
    // 배치 산출 — 스케줄러 호출 (외부 API 호출을 트랜잭션 밖에서 수행)
    // ──────────────────────────────────────────────

    @Transactional
    public void calculateAndSaveRankings() {
        log.info("섹터 랭킹 산출 시작");

        // FRED 금리 데이터 사전 조회 (트랜잭션 밖 — DB 커넥션 점유 방지)
        MacroRegime regime = fetchMacroRegime();

        List<Sector> sectors = sectorRepository.findAll();
        LocalDateTime now = LocalDateTime.now();

        List<SectorScore> scored = new ArrayList<>();
        for (Sector sector : sectors) {
            try {
                Optional<BigDecimal> revenueGrowth =
                        financialMetricRepository.avgLatestRevenueGrowthBySector(sector.getId());

                if (revenueGrowth.isEmpty()) {
                    log.info("섹터 랭킹 제외 — 재무 데이터 없음: sector={}", sector.getCode());
                    continue;
                }

                BigDecimal newsVolumeScore    = computeNewsVolumeScore(sector.getId(), now);
                BigDecimal sentimentScore     = computeSentimentScore(sector.getId(), now);
                BigDecimal revenueGrowthScore = normalizeRevenueGrowth(revenueGrowth.get());
                BigDecimal macroScore         = computeMacroScore(sector.getCode(), regime);
                BigDecimal momentumScore      = computeMomentumScore(sector.getId());

                BigDecimal total = newsVolumeScore.multiply(BigDecimal.valueOf(W_NEWS_VOLUME))
                        .add(sentimentScore.multiply(BigDecimal.valueOf(W_SENTIMENT)))
                        .add(revenueGrowthScore.multiply(BigDecimal.valueOf(W_REVENUE_GROWTH)))
                        .add(macroScore.multiply(BigDecimal.valueOf(W_MACRO)))
                        .add(momentumScore.multiply(BigDecimal.valueOf(W_MOMENTUM)))
                        .setScale(2, RoundingMode.HALF_UP);

                scored.add(new SectorScore(sector, total));
                log.debug("섹터 점수 산출 — sector={}, total={}, newsVol={}, sentiment={}, revGrowth={}, macro={}, momentum={}",
                        sector.getCode(), total, newsVolumeScore, sentimentScore,
                        revenueGrowthScore, macroScore, momentumScore);

            } catch (Exception e) {
                log.error("섹터 점수 산출 실패 — sector={}", sector.getCode(), e);
            }
        }

        scored.sort(Comparator.comparing(SectorScore::score).reversed());

        for (int i = 0; i < scored.size(); i++) {
            Sector sector = scored.get(i).sector();
            sector.updateRanking(i + 1, scored.get(i).score());
            sectorRepository.save(sector);
        }

        log.info("섹터 랭킹 산출 완료: {}개 섹터 산출", scored.size());
    }

    // ──────────────────────────────────────────────
    // 점수 컴포넌트
    // ──────────────────────────────────────────────

    /**
     * 뉴스 볼륨 추이 점수 (25%).
     * 최근 7일 vs 직전 7일 증가율 → 0~100 정규화.
     * 증가율 0% → 50점, +100% → 100점, -100% → 0점 (선형 클램프).
     */
    private BigDecimal computeNewsVolumeScore(Long sectorId, LocalDateTime now) {
        LocalDateTime sevenDaysAgo     = now.minusDays(7);
        LocalDateTime fourteenDaysAgo  = now.minusDays(14);

        long recent = newsArticleRepository.countBySectorAndPeriod(sectorId, sevenDaysAgo, now);
        long prior  = newsArticleRepository.countBySectorAndPeriod(sectorId, fourteenDaysAgo, sevenDaysAgo);

        if (prior == 0 && recent == 0) return BigDecimal.valueOf(50); // 데이터 없음 → 중립
        if (prior == 0) return HUNDRED;

        double changeRate = (double)(recent - prior) / prior;
        double clamped    = Math.max(-1.0, Math.min(1.0, changeRate));
        return BigDecimal.valueOf(50 * (1 + clamped)).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 평균 감정 점수 (20%).
     * 최근 14일 뉴스 평균 sentiment (-1~1) → 0~100 정규화.
     * 데이터 없으면 50 (중립).
     */
    private BigDecimal computeSentimentScore(Long sectorId, LocalDateTime now) {
        BigDecimal avg = newsArticleRepository.avgSentimentBySectorAndPeriod(
                sectorId, now.minusDays(14), now);

        if (avg == null) return BigDecimal.valueOf(50);
        // (-1~1) → (0~100)
        return avg.add(BigDecimal.ONE)
                .divide(BigDecimal.valueOf(2), 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 매출성장률 정규화 (25%). -20% → 0, +100% → 100.
     */
    private BigDecimal normalizeRevenueGrowth(BigDecimal yoy) {
        BigDecimal clamped = yoy.max(REVENUE_MIN).min(REVENUE_MAX);
        return clamped.subtract(REVENUE_MIN)
                .divide(REVENUE_RANGE, 4, RoundingMode.HALF_UP)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 거시 연관성 점수 (20%).
     * DFF 기준 금리 환경 → 섹터별 하드코딩 점수 조회.
     */
    private BigDecimal computeMacroScore(String sectorCode, MacroRegime regime) {
        double[] scores = MACRO_SCORE_TABLE.getOrDefault(sectorCode, DEFAULT_MACRO_SCORE);
        return BigDecimal.valueOf(scores[regime.ordinal()]).setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 모멘텀 점수 (10%).
     * 섹터 내 종목 중 최근 분기 영업이익률이 직전 분기 대비 개선된 비율 × 100.
     * 데이터 없으면 50 (중립).
     */
    private BigDecimal computeMomentumScore(Long sectorId) {
        List<Stock> stocks = stockRepository.findBySectorIdAndMarketOrderByMarketCapDesc(sectorId, "US");
        if (stocks.isEmpty()) return BigDecimal.valueOf(50);

        int improved = 0;
        int withData = 0;
        for (Stock stock : stocks) {
            List<FinancialMetric> twoQuarters =
                    financialMetricRepository.findTop2ByStockIdAndPeriodOrderByFiscalDateDesc(stock.getId(), "quarterly");
            if (twoQuarters.size() < 2) continue;

            BigDecimal latest = twoQuarters.get(0).getOperatingMargin();
            BigDecimal prev   = twoQuarters.get(1).getOperatingMargin();
            if (latest == null || prev == null) continue;

            withData++;
            if (latest.compareTo(prev) > 0) improved++;
        }

        if (withData == 0) return BigDecimal.valueOf(50);
        return BigDecimal.valueOf((double) improved / withData)
                .multiply(HUNDRED)
                .setScale(2, RoundingMode.HALF_UP);
    }

    // ──────────────────────────────────────────────
    // FRED 금리 환경 판단
    // ──────────────────────────────────────────────

    private MacroRegime fetchMacroRegime() {
        try {
            RawMacroData dff = macroDataService.fetchCached(FRED_DFF_SERIES);
            if (dff == null || dff.value() == null) {
                log.warn("FRED DFF 값 없음, 중립 금리 환경(MEDIUM_RATE)으로 가정");
                return MacroRegime.MEDIUM_RATE;
            }
            return determineRateRegime(dff.value());
        } catch (Exception e) {
            log.warn("FRED 금리 데이터 조회 실패 — 중립 금리 환경(MEDIUM_RATE)으로 가정: {}", e.getMessage());
            return MacroRegime.MEDIUM_RATE;
        }
    }

    private MacroRegime determineRateRegime(BigDecimal dff) {
        if (dff.compareTo(RATE_LOW_THRESHOLD) < 0)  return MacroRegime.LOW_RATE;
        if (dff.compareTo(RATE_HIGH_THRESHOLD) > 0) return MacroRegime.HIGH_RATE;
        return MacroRegime.MEDIUM_RATE;
    }

    // ──────────────────────────────────────────────
    // 응답 변환 헬퍼
    // ──────────────────────────────────────────────

    private SectorRankItem toRankItem(Sector sector, LocalDateTime now) {
        LocalDateTime fourteenDaysAgo = now.minusDays(14);
        LocalDateTime sevenDaysAgo    = now.minusDays(7);

        long recent = newsArticleRepository.countBySectorAndPeriod(sector.getId(), sevenDaysAgo, now);
        long prior  = newsArticleRepository.countBySectorAndPeriod(sector.getId(), fourteenDaysAgo, sevenDaysAgo);

        BigDecimal avgSentiment    = newsArticleRepository.avgSentimentBySectorAndPeriod(
                sector.getId(), fourteenDaysAgo, now);
        BigDecimal avgRevenueGrowth = financialMetricRepository
                .avgLatestRevenueGrowthBySector(sector.getId()).orElse(null);

        Highlights highlights = new Highlights(
                formatNewsVolumeChange(recent, prior),
                avgSentiment,
                avgRevenueGrowth
        );

        return new SectorRankItem(
                sector.getId(),
                sector.getLatestRank(),
                sector.getCode(),
                sector.getName(),
                sector.getLatestScore(),
                highlights
        );
    }

    private String formatNewsVolumeChange(long recent, long prior) {
        if (prior == 0 && recent == 0) return "0%";
        if (prior == 0) return "+100%+";
        double changeRate = (double)(recent - prior) / prior * 100;
        return String.format("%+.0f%%", changeRate);
    }

    private record SectorScore(Sector sector, BigDecimal score) {}
}
