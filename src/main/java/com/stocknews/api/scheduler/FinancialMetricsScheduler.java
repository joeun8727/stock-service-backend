package com.stocknews.api.scheduler;

import com.stocknews.api.client.financial.FinancialProvider;
import com.stocknews.api.client.financial.RawFinancialMetric;
import com.stocknews.api.client.financial.RawStockProfile;
import com.stocknews.api.common.config.CacheKeys;
import com.stocknews.api.domain.financial.FinancialMetric;
import com.stocknews.api.domain.financial.FinancialMetricRepository;
import com.stocknews.api.domain.sector.ScoringService;
import com.stocknews.api.domain.sector.TrendCalculator;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinancialMetricsScheduler {

    private static final String PERIOD = "quarterly";

    private final FinancialProvider         financialProvider;
    private final StockRepository           stockRepository;
    private final FinancialMetricRepository financialMetricRepository;
    private final TrendCalculator           trendCalculator;
    private final ScoringService            scoringService;

    @Scheduled(cron = "${scheduler.financial-metrics.cron}")
    @CacheEvict(value = {CacheKeys.SECTOR_LARGECAP, CacheKeys.SECTOR_GROWTH}, allEntries = true)
    public void collectFinancialMetrics() {
        log.info("재무지표 수집 스케줄러 시작");

        List<Stock> stocks = stockRepository.findAllByMarket("US");
        if (stocks.isEmpty()) {
            log.info("수집 대상 종목 없음 — 스케줄러 종료");
            return;
        }

        LocalDate fiscalDate = startOfCurrentQuarter();
        int profileUpdated = 0;
        int metricsStored  = 0;

        for (Stock stock : stocks) {
            try {
                RawStockProfile profile = financialProvider.fetchProfile(stock.getTicker());
                if (profile != null) {
                    stock.updateProfile(profile.companyName(), profile.industry(),
                            profile.marketCapMillions(), profile.exchange(),
                            profile.website(), profile.ipoDate());
                    stockRepository.save(stock);
                    profileUpdated++;
                }

                if (financialMetricRepository.existsByStockIdAndPeriodAndFiscalDate(
                        stock.getId(), PERIOD, fiscalDate)) {
                    log.debug("재무지표 이미 수집됨 — ticker={}", stock.getTicker());
                    continue;
                }

                List<RawFinancialMetric> metrics = financialProvider.fetchMetrics(stock.getTicker());
                if (metrics.isEmpty()) continue;

                RawFinancialMetric raw = metrics.get(0);
                saveMetricAndTrends(stock, raw, fiscalDate);
                metricsStored++;

            } catch (Exception e) {
                log.error("재무지표 수집 예외 — ticker={}", stock.getTicker(), e);
            }
        }

        log.info("재무지표 수집 완료: 프로필 갱신 {}건, 지표 신규 저장 {}건", profileUpdated, metricsStored);
        runScoring();
        log.info("재무지표 수집 스케줄러 완료");
    }

    @CacheEvict(value = {CacheKeys.SECTOR_LARGECAP, CacheKeys.SECTOR_GROWTH}, allEntries = true)
    public void collectMissingMetrics() {
        LocalDate fiscalDate = startOfCurrentQuarter();
        List<Stock> missing = financialMetricRepository.findStocksWithoutMetrics("US", PERIOD, fiscalDate);

        if (missing.isEmpty()) {
            log.info("미수집 재무지표 없음 — 모든 종목 수집 완료");
            return;
        }

        log.info("미수집 재무지표 수집 시작 — 대상 {}건", missing.size());
        int profileUpdated = 0;
        int metricsStored  = 0;

        for (Stock stock : missing) {
            try {
                RawStockProfile profile = financialProvider.fetchProfile(stock.getTicker());
                if (profile != null) {
                    stock.updateProfile(profile.companyName(), profile.industry(),
                            profile.marketCapMillions(), profile.exchange(),
                            profile.website(), profile.ipoDate());
                    stockRepository.save(stock);
                    profileUpdated++;
                }

                List<RawFinancialMetric> metrics = financialProvider.fetchMetrics(stock.getTicker());
                if (metrics.isEmpty()) {
                    log.debug("재무지표 없음 (Finnhub 미제공) — ticker={}", stock.getTicker());
                    continue;
                }

                RawFinancialMetric raw = metrics.get(0);
                saveMetricAndTrends(stock, raw, fiscalDate);
                metricsStored++;

            } catch (Exception e) {
                log.error("미수집 재무지표 수집 예외 — ticker={}", stock.getTicker(), e);
            }
        }

        log.info("미수집 재무지표 수집 완료: 프로필 갱신 {}건, 지표 신규 저장 {}건", profileUpdated, metricsStored);
        runScoring();
    }

    // ── 내부 ────────────────────────────────────────────────────────────────

    private void saveMetricAndTrends(Stock stock, RawFinancialMetric raw, LocalDate fiscalDate) {
        financialMetricRepository.save(FinancialMetric.builder()
                .stock(stock).period(PERIOD).fiscalDate(fiscalDate)
                .roe(raw.roe()).roa(raw.roa()).roic(raw.roic())
                .per(raw.per()).pbr(raw.pbr()).eps(raw.eps())
                .debtRatio(raw.debtRatio()).interestCoverage(raw.interestCoverage())
                .revenueGrowthYoy(raw.revenueGrowthYoy())
                .operatingMargin(raw.operatingMargin()).ocfToNi(raw.ocfToNi())
                .psr(raw.psr()).peg(raw.peg())
                .grossMargin(raw.grossMargin()).fcfMargin(raw.fcfMargin())
                .build());

        // 시계열 추세 계산 + MetricTrend 저장
        trendCalculator.calculateAndStore(stock, "grossMargin",     raw.grossMarginSeries());
        trendCalculator.calculateAndStore(stock, "roic",            raw.roicSeries());
        trendCalculator.calculateAndStore(stock, "operatingMargin", raw.operatingMarginSeries());
        trendCalculator.calculateAndStore(stock, "fcfMargin",       raw.fcfMarginSeries());
    }

    private void runScoring() {
        try {
            scoringService.scoreAllSectors();
        } catch (Exception e) {
            log.error("섹터 스코어링 실패", e);
        }
    }

    private static LocalDate startOfCurrentQuarter() {
        LocalDate today = LocalDate.now();
        Month firstMonthOfQuarter = today.getMonth().firstMonthOfQuarter();
        return today.withMonth(firstMonthOfQuarter.getValue()).withDayOfMonth(1);
    }
}
