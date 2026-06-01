package com.stocknews.api.scheduler;

import com.stocknews.api.client.financial.FinancialProvider;
import com.stocknews.api.client.financial.FinnhubSymbolItem;
import com.stocknews.api.client.financial.RawStockProfile;
import com.stocknews.api.domain.sector.IndustryToSectorMapper;
import com.stocknews.api.domain.sector.Sector;
import com.stocknews.api.domain.sector.SectorRepository;
import com.stocknews.api.domain.sector.ScoringWeights;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 종목 마스터 동적 구성 배치 — 매일 1회.
 *
 * 1. /stock/symbol?exchange=US 로 미국 종목 풀 확보
 * 2. 신규/미갱신 종목 프로필 수집 (finnhubIndustry → Sector 매핑)
 * 3. 섹터별 시총 상위 N개 보장, 나머지는 DB에 유지(삭제 안 함)
 *
 * Rate Limit: Resilience4j RateLimiter가 자동 조절 (55 req/min).
 * CB 오픈 시 해당 종목 건너뛰고 다음 실행에 재시도.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StockMasterScheduler {

    private static final String EXCHANGE = "US";

    private final FinancialProvider    financialProvider;
    private final StockRepository      stockRepository;
    private final SectorRepository     sectorRepository;
    private final IndustryToSectorMapper industryMapper;

    @Scheduled(cron = "${scheduler.stock-master.cron}")
    public void collectStockMaster() {
        log.info("종목 마스터 수집 스케줄러 시작");

        List<FinnhubSymbolItem> symbols = financialProvider.fetchSymbols(EXCHANGE);
        if (symbols.isEmpty()) {
            log.warn("종목 목록 조회 실패 또는 결과 없음 — 스케줄러 종료");
            return;
        }

        // Common Stock, USD만 처리
        List<String> tickers = symbols.stream()
                .filter(s -> "Common Stock".equals(s.type()) && "USD".equals(s.currency()))
                .filter(s -> s.symbol() != null && !s.symbol().contains(".")) // 클래스 주식 제외
                .map(FinnhubSymbolItem::symbol)
                .collect(Collectors.toList());

        log.info("종목 목록 확보: 전체 {}개 → 필터 후 {}개", symbols.size(), tickers.size());

        // 이미 DB에 있는 종목 티커 세트
        Set<String> existingTickers = stockRepository.findAllByMarket("US").stream()
                .map(Stock::getTicker).collect(Collectors.toSet());

        // 신규 종목만 프로필 수집
        List<String> newTickers = tickers.stream()
                .filter(t -> !existingTickers.contains(t))
                .collect(Collectors.toList());

        log.info("신규 종목 프로필 수집 대상: {}개", newTickers.size());

        Map<String, Sector> sectorByCode = sectorRepository.findAll().stream()
                .collect(Collectors.toMap(Sector::getCode, s -> s));

        // 섹터별 시총 추적 (상위 N개 보장용)
        Map<String, List<StockCapEntry>> sectorCandidates = new HashMap<>();
        int profiled = 0;
        int upserted = 0;

        for (String ticker : newTickers) {
            try {
                RawStockProfile profile = financialProvider.fetchProfile(ticker);
                if (profile == null) continue;
                profiled++;

                Optional<String> sectorCode = industryMapper.toSectorCode(profile.industry());
                if (sectorCode.isEmpty()) {
                    log.debug("섹터 매핑 없음 — ticker={}, industry={}", ticker, profile.industry());
                    continue;
                }

                Sector sector = sectorByCode.get(sectorCode.get());
                if (sector == null) continue;

                BigDecimal cap = profile.marketCapMillions() != null
                        ? profile.marketCapMillions().multiply(BigDecimal.valueOf(1_000_000))
                        : null;

                sectorCandidates.computeIfAbsent(sectorCode.get(), k -> new ArrayList<>())
                        .add(new StockCapEntry(ticker, profile, sector, cap));

            } catch (Exception e) {
                log.debug("프로필 수집 실패 — ticker={}, 원인={}", ticker, e.getMessage());
            }
        }

        // 섹터별 시총 상위 N개만 DB 저장
        int topN = ScoringWeights.STOCK_MASTER_TOP_N_PER_SECTOR;
        for (Map.Entry<String, List<StockCapEntry>> entry : sectorCandidates.entrySet()) {
            List<StockCapEntry> sorted = entry.getValue().stream()
                    .sorted(Comparator.comparing(
                            e -> e.cap != null ? e.cap : BigDecimal.ZERO,
                            Comparator.reverseOrder()))
                    .limit(topN)
                    .toList();

            for (StockCapEntry e : sorted) {
                try {
                    Optional<Stock> existing = stockRepository.findByMarketAndTicker("US", e.ticker);
                    if (existing.isPresent()) {
                        existing.get().updateProfile(
                                e.profile.companyName(), e.profile.industry(),
                                e.profile.marketCapMillions(), e.profile.exchange(),
                                e.profile.website(), e.profile.ipoDate());
                        stockRepository.save(existing.get());
                    } else {
                        stockRepository.save(Stock.builder()
                                .ticker(e.ticker).market("US")
                                .companyName(e.profile.companyName())
                                .sector(e.sector)
                                .industry(e.profile.industry())
                                .marketCap(e.cap)
                                .exchange(e.profile.exchange())
                                .website(e.profile.website())
                                .ipoDate(e.profile.ipoDate())
                                .build());
                        upserted++;
                    }
                } catch (Exception ex) {
                    log.error("종목 저장 예외 — ticker={}", e.ticker, ex);
                }
            }
        }

        log.info("종목 마스터 수집 완료: 프로필 조회 {}건, 신규 저장 {}건", profiled, upserted);
    }

    private record StockCapEntry(String ticker, RawStockProfile profile,
                                  Sector sector, BigDecimal cap) {}
}
