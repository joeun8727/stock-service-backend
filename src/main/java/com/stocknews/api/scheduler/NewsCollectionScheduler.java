package com.stocknews.api.scheduler;

import com.stocknews.api.domain.news.NewsCollectionService;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

/**
 * 뉴스 수집 스케줄러 — 매 정시 Finnhub 수집만 담당 (06-news-llm.md).
 * LLM 분석은 LlmAnalysisScheduler가 분당 8건씩 별도 처리.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NewsCollectionScheduler {

    private final NewsCollectionService newsCollectionService;
    private final StockRepository stockRepository;

    @Value("${news.collect.lookback-days}") private int lookbackDays;

    @Scheduled(cron = "${scheduler.news-collection.cron}")
    public void collectNews() {
        log.info("뉴스 수집 스케줄러 시작");

        List<Stock> stocks = stockRepository.findAllByMarket("US");
        if (stocks.isEmpty()) {
            log.info("수집 대상 종목 없음 — 스케줄러 종료");
            return;
        }

        LocalDate from = LocalDate.now().minusDays(lookbackDays);
        LocalDate to   = LocalDate.now();

        int totalSaved = 0;
        for (Stock stock : stocks) {
            try {
                totalSaved += newsCollectionService.collectForTicker(stock, from, to);
            } catch (Exception e) {
                log.error("뉴스 수집 예외 — ticker={}", stock.getTicker(), e);
            }
        }
        log.info("Finnhub 수집 완료: {}개 종목, 신규 {}건 저장", stocks.size(), totalSaved);
    }
}
