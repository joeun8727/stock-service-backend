package com.stocknews.api.scheduler;

import com.stocknews.api.domain.apicalllog.ApiCallLogRepository;
import com.stocknews.api.domain.news.NewsCollectionService;
import com.stocknews.api.domain.news.SnippetQueue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * LLM 분석 스케줄러 — 분당 최대 8건 처리 (06-news-llm.md).
 *
 * 처리 우선순위:
 *   1. SnippetQueue (snippet 있음 → 분석 품질 우수)
 *   2. DB 큐 fallback (llm_processed=false, headline만)
 *
 * 분당 8건 이하로 제한해 Gemini 10 RPM 한도 준수.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmAnalysisScheduler {

    private final NewsCollectionService newsCollectionService;
    private final ApiCallLogRepository apiCallLogRepository;
    private final SnippetQueue snippetQueue;

    @Value("${news.gemini.daily-limit}")      private int geminiDailyLimit;
    @Value("${news.gemini.per-minute-batch}") private int perMinuteBatch;

    @Scheduled(fixedDelayString = "${scheduler.llm-analysis.fixed-delay-ms}")
    public void processLlmBatch() {
        long todayCallCount = apiCallLogRepository
                .countByProviderAndCalledAtAfter("gemini",
                        LocalDateTime.now().toLocalDate().atStartOfDay());

        int remaining = Math.max(0, geminiDailyLimit - (int) todayCallCount);
        if (remaining <= 0) {
            log.debug("Gemini 일일 한도 소진({}/{}) — LLM 처리 스킵", todayCallCount, geminiDailyLimit);
            return;
        }

        int batchSize = Math.min(remaining, perMinuteBatch);
        int processed = 0;

        // 1. SnippetQueue 우선 처리 (snippet → LLM 품질 높음)
        while (processed < batchSize && !snippetQueue.isEmpty()) {
            SnippetQueue.SnippetEntry entry = snippetQueue.poll();
            if (entry == null) break;
            boolean canContinue = newsCollectionService.processQueuedArticle(entry);
            processed++;
            if (!canContinue) {
                log.info("LLM 분석 배치 중단 (rate limit) — 큐 처리 {}건, 잔여 큐 {}건",
                        processed, snippetQueue.size());
                return;
            }
        }

        // 2. DB 큐 fallback — 잔여 용량만큼 headline 기반 처리
        int remaining_capacity = batchSize - processed;
        if (remaining_capacity > 0) {
            boolean hasMore = newsCollectionService.processNextLlmBatch(remaining_capacity);
            log.debug("LLM DB 배치 완료 — {}건 처리, 잔여={}", remaining_capacity, hasMore);
        }
    }
}
