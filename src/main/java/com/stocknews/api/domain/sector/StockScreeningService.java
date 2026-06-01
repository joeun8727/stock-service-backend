package com.stocknews.api.domain.sector;

import com.stocknews.api.common.config.CacheKeys;
import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.domain.financial.FinancialMetric;
import com.stocknews.api.domain.financial.FinancialMetricRepository;
import com.stocknews.api.domain.sector.dto.SectorStocksResponse;
import com.stocknews.api.domain.sector.dto.SectorStocksResponse.MetricsSnapshot;
import com.stocknews.api.domain.sector.dto.SectorStocksResponse.StockScreenResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 섹터별 종목 스크리닝 API — StockScore 테이블 조회 기반.
 * 실시간 계산 금지. 배치(ScoringService)가 미리 계산한 결과만 반환.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockScreeningService {

    private static final String PERIOD = "quarterly";

    private final SectorRepository          sectorRepository;
    private final StockScoreRepository      stockScoreRepository;
    private final FinancialMetricRepository financialMetricRepository;
    private final ScoringService            scoringService;

    // ──────────────────────────────────────────────
    // GET /api/v1/sectors/{sectorId}/stocks?type=large_cap
    // ──────────────────────────────────────────────

    @Cacheable(value = CacheKeys.SECTOR_LARGECAP, key = "#sectorId + '_' + #limit")
    public SectorStocksResponse getLargeCapStocks(Long sectorId, int limit) {
        Sector sector = findSector(sectorId);
        List<StockScore> scores = stockScoreRepository
                .findBySectorIdAndScreenTypeOrderByRank(sectorId, "LARGE_CAP");

        if (scores.isEmpty()) {
            log.warn("대형주 스코어 없음 — sectorId={}", sectorId);
        }

        List<StockScreenResult> results = buildResults(scores, limit, false);
        return new SectorStocksResponse(sectorId, sector.getCode(), sector.getName(), "large_cap", results);
    }

    // ──────────────────────────────────────────────
    // GET /api/v1/sectors/{sectorId}/stocks?type=growth
    // ──────────────────────────────────────────────

    @Cacheable(value = CacheKeys.SECTOR_GROWTH, key = "#sectorId + '_' + #limit")
    public SectorStocksResponse getGrowthStocks(Long sectorId, int limit) {
        Sector sector = findSector(sectorId);
        List<StockScore> scores = stockScoreRepository
                .findBySectorIdAndScreenTypeOrderByRank(sectorId, "GROWTH");

        if (scores.isEmpty()) {
            log.warn("성장 지표 스코어 없음 — sectorId={}", sectorId);
        }

        List<StockScreenResult> results = buildResults(scores, limit, true);
        return new SectorStocksResponse(sectorId, sector.getCode(), sector.getName(), "growth", results);
    }

    // ──────────────────────────────────────────────
    // 스케줄러 호출 — ScoringService 위임
    // ──────────────────────────────────────────────

    @Transactional
    public void refreshAllGrowthCandidateFlags() {
        scoringService.scoreAllSectors();
    }

    // ──────────────────────────────────────────────
    // 내부 헬퍼
    // ──────────────────────────────────────────────

    private List<StockScreenResult> buildResults(List<StockScore> scores,
                                                  int limit, boolean includeScore) {
        List<StockScreenResult> results = new ArrayList<>();
        for (StockScore ss : scores) {
            if (results.size() >= limit) break;

            Optional<FinancialMetric> metricOpt = financialMetricRepository
                    .findTopByStockIdAndPeriodOrderByFiscalDateDesc(ss.getStock().getId(), PERIOD);

            MetricsSnapshot snapshot = metricOpt.map(this::toSnapshot).orElse(null);
            results.add(new StockScreenResult(
                    ss.getRankInSector(),
                    ss.getStock().getTicker(),
                    ss.getStock().getCompanyName(),
                    ss.getStock().getMarketCap(),
                    snapshot,
                    includeScore ? ss.getTotalScore() : null
            ));
        }
        return results;
    }

    private Sector findSector(Long sectorId) {
        return sectorRepository.findById(sectorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.SECTOR_NOT_FOUND));
    }

    private MetricsSnapshot toSnapshot(FinancialMetric m) {
        return new MetricsSnapshot(
                m.getRoe(), m.getRoa(), m.getRoic(),
                m.getPer(), m.getPbr(),
                m.getRevenueGrowthYoy(), m.getOperatingMargin(),
                m.getPsr(), m.getPeg(), m.getGrossMargin(), m.getFcfMargin()
        );
    }
}
