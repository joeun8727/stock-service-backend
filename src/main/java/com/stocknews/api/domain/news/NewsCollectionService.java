package com.stocknews.api.domain.news;

import com.stocknews.api.client.llm.LLMAnalysis;
import com.stocknews.api.client.llm.LLMClient;
import com.stocknews.api.client.news.NewsProvider;
import com.stocknews.api.client.news.RawNews;
import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.domain.stock.Stock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 뉴스 수집 + LLM 분석 파이프라인 (06-news-llm.md).
 *
 * 수집 단계: Finnhub → 중복 제거 → 키워드 필터 → DB 저장(llm_processed=false)
 *            + snippet을 SnippetQueue에 적재 (DB 저장 금지 — 08-compliance.md)
 *
 * LLM 분석 단계 (LlmAnalysisScheduler 호출):
 *   - processQueuedArticle(): snippet 있는 기사 (우선)
 *   - processNextLlmBatch(): DB 큐 fallback (headline만, snippet 없는 기사)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NewsCollectionService {

    private final NewsProvider newsProvider;
    private final LLMClient llmClient;
    private final NewsArticleRepository newsArticleRepository;
    private final NewsKeywordFilter keywordFilter;
    private final SnippetQueue snippetQueue;

    // ────────────────────────────────────────────
    // 수집: Finnhub → DB 저장 + snippet 큐 적재
    // ────────────────────────────────────────────

    public int collectForTicker(Stock stock, LocalDate from, LocalDate to) {
        List<RawNews> rawNewsList;
        try {
            rawNewsList = newsProvider.fetchCompanyNews(stock.getTicker(), from, to);
        } catch (Exception e) {
            log.error("Finnhub 뉴스 수집 실패 — ticker={}, 원인={}", stock.getTicker(), e.getMessage());
            return 0;
        }

        int saved = 0;
        for (RawNews raw : rawNewsList) {
            if (newsArticleRepository.existsBySourceUrl(raw.sourceUrl())) continue;
            if (!keywordFilter.passes(raw.headline(), raw.snippet())) continue;

            // 원문 본문 저장 금지 — 헤드라인·메타만 저장 (08-compliance.md)
            NewsArticle article = newsArticleRepository.save(
                    NewsArticle.builder()
                            .stock(stock)
                            .source(raw.source())
                            .sourceUrl(raw.sourceUrl())
                            .headline(raw.headline())
                            .publishedAt(raw.publishedAt())
                            .build()
            );

            // snippet은 인메모리 큐에만 적재 → LLM 분석 후 자동 폐기
            if (raw.snippet() != null && !raw.snippet().isBlank()) {
                snippetQueue.offer(article.getId(), raw.snippet());
            }
            saved++;
        }
        log.info("뉴스 수집 완료 — ticker={}, 저장={}건", stock.getTicker(), saved);
        return saved;
    }

    // ────────────────────────────────────────────
    // LLM 분석 ① — snippet 큐 기반 (우선 처리)
    // ────────────────────────────────────────────

    /**
     * SnippetQueue에서 기사 1건을 꺼내 LLM 분석.
     *
     * @return false = rate limit / 서킷 오픈 (배치 중단), true = 계속 가능
     */
    public boolean processQueuedArticle(SnippetQueue.SnippetEntry entry) {
        NewsArticle article = newsArticleRepository.findById(entry.articleId()).orElse(null);
        if (article == null || Boolean.TRUE.equals(article.getLlmProcessed())) {
            return true; // 이미 처리됐거나 삭제됨, 건너뜀
        }

        try {
            LLMAnalysis analysis = llmClient.analyze(article.getHeadline(), entry.snippet());
            return analyzeAndSave(article, analysis);
        } catch (BusinessException e) {
            if (isRateLimitOrCircuitOpen(e)) {
                log.warn("Gemini rate limit / 서킷 오픈 — 큐 배치 중단 (articleId={})", article.getId());
                return false;
            }
            log.error("LLM 분석 실패 — articleId={}", article.getId(), e);
            return true;
        } catch (Exception e) {
            log.error("LLM 분석 예외 — articleId={}", article.getId(), e);
            return true;
        }
    }

    // ────────────────────────────────────────────
    // LLM 분석 ② — DB 큐 fallback (headline만)
    // ────────────────────────────────────────────

    /**
     * llm_processed=false 기사를 오래된 순으로 batchSize개 분석.
     *
     * @return 처리할 기사가 더 있으면 true
     */
    public boolean processNextLlmBatch(int batchSize) {
        List<NewsArticle> pending = newsArticleRepository
                .findByLlmProcessedFalseOrderByCreatedAtAsc(PageRequest.of(0, batchSize))
                .getContent();

        if (pending.isEmpty()) return false;

        for (NewsArticle article : pending) {
            try {
                LLMAnalysis analysis = llmClient.analyze(article.getHeadline(), null);
                boolean ok = analyzeAndSave(article, analysis);
                if (!ok) return true; // rate limit
            } catch (BusinessException e) {
                if (isRateLimitOrCircuitOpen(e)) {
                    log.warn("Gemini rate limit / 서킷 오픈 — DB 배치 중단, 다음 주기 재시도");
                    return true;
                }
                log.error("LLM 분석 실패 — articleId={}, llm_processed=false 유지", article.getId(), e);
            } catch (Exception e) {
                log.error("LLM 분석 예외 — articleId={}", article.getId(), e);
            }
        }
        return pending.size() == batchSize;
    }

    // ────────────────────────────────────────────
    // 공통: 검증 + 저장
    // ────────────────────────────────────────────

    /**
     * LLM 결과 검증 후 저장.
     * 검증 실패 시 llm_processed=false 유지 → 다음 배치에서 재처리.
     *
     * @return false = rate limit (배치 중단), true = 계속 가능
     */
    private boolean analyzeAndSave(NewsArticle article, LLMAnalysis analysis) {
        // importance null → 파싱 실패, 재처리
        if (analysis.importance() == null) {
            log.warn("importance 파싱 실패 — 재처리 예약 (articleId={})", article.getId());
            return true;
        }

        // relevance-importance 일관성 검증
        if (isInconsistent(analysis)) {
            log.warn("relevance-importance 불일치 (relevance={}, importance={}) — 재처리 예약 (articleId={})",
                    analysis.relevance(), analysis.importance(), article.getId());
            return true;
        }

        article.applyLlmAnalysis(
                analysis.summary(),
                analysis.sentiment(),
                analysis.importance(),
                analysis.relevance()
        );
        newsArticleRepository.save(article);
        return true;
    }

    /**
     * relevance=HIGH → importance>=60, MEDIUM → importance>=30 위반 여부.
     */
    private boolean isInconsistent(LLMAnalysis a) {
        return ("HIGH".equals(a.relevance())   && a.importance() < 60)
            || ("MEDIUM".equals(a.relevance()) && a.importance() < 30);
    }

    private boolean isRateLimitOrCircuitOpen(BusinessException e) {
        return e.getErrorCode() == ErrorCode.EXTERNAL_API_RATE_LIMITED
                || e.getErrorCode() == ErrorCode.EXTERNAL_API_CIRCUIT_OPEN;
    }
}
