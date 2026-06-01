package com.stocknews.api.domain.news;

import com.stocknews.api.client.llm.LLMAnalysis;
import com.stocknews.api.client.llm.LLMClient;
import com.stocknews.api.client.news.NewsProvider;
import com.stocknews.api.client.news.RawNews;
import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.domain.stock.Stock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NewsCollectionServiceTest {

    @Mock NewsProvider newsProvider;
    @Mock LLMClient llmClient;
    @Mock NewsArticleRepository newsArticleRepository;

    NewsKeywordFilter keywordFilter;
    NewsCollectionService service;
    Stock stock;

    @BeforeEach
    void setUp() {
        keywordFilter = new NewsKeywordFilter("earnings,revenue,guidance");
        service = new NewsCollectionService(newsProvider, llmClient, newsArticleRepository, keywordFilter);
        stock = Stock.builder().ticker("AAPL").market("US").companyName("Apple Inc.").build();
    }

    // ──────────────────────────────────────────────
    // collectForTicker
    // ──────────────────────────────────────────────

    @Test
    void collectForTicker_키워드_통과_뉴스만_저장() {
        RawNews matching    = raw("Apple earnings beat expectations", "https://r.co/1", "Earnings Q1");
        RawNews nonMatching = raw("Apple opens new store in Seoul", "https://r.co/2", null);

        when(newsProvider.fetchCompanyNews(eq("AAPL"), any(), any()))
                .thenReturn(List.of(matching, nonMatching));
        when(newsArticleRepository.existsBySourceUrl(any())).thenReturn(false);
        when(newsArticleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        int saved = service.collectForTicker(stock, LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(saved).isEqualTo(1);
        verify(newsArticleRepository, times(1)).save(any(NewsArticle.class));
    }

    @Test
    void collectForTicker_중복_sourceUrl_건너뜀() {
        RawNews news = raw("Apple revenue guidance", "https://r.co/dup", "revenue up");
        when(newsProvider.fetchCompanyNews(any(), any(), any())).thenReturn(List.of(news));
        when(newsArticleRepository.existsBySourceUrl("https://r.co/dup")).thenReturn(true);

        int saved = service.collectForTicker(stock, LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(saved).isZero();
        verify(newsArticleRepository, never()).save(any());
    }

    @Test
    void collectForTicker_저장된_NewsArticle_원문_본문_없음() {
        RawNews news = raw("Apple Q1 earnings results", "https://r.co/ok", "earnings beat");
        when(newsProvider.fetchCompanyNews(any(), any(), any())).thenReturn(List.of(news));
        when(newsArticleRepository.existsBySourceUrl(any())).thenReturn(false);
        when(newsArticleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.collectForTicker(stock, LocalDate.now().minusDays(1), LocalDate.now());

        ArgumentCaptor<NewsArticle> captor = ArgumentCaptor.forClass(NewsArticle.class);
        verify(newsArticleRepository).save(captor.capture());
        NewsArticle saved = captor.getValue();
        assertThat(saved.getSummary()).isNull();        // LLM 미처리, 원문 본문 없음
        assertThat(saved.getLlmProcessed()).isFalse();
    }

    @Test
    void collectForTicker_Finnhub_예외_시_0_반환() {
        when(newsProvider.fetchCompanyNews(any(), any(), any()))
                .thenThrow(new RuntimeException("Finnhub 연결 실패"));

        int saved = service.collectForTicker(stock, LocalDate.now().minusDays(1), LocalDate.now());

        assertThat(saved).isZero();
        verify(newsArticleRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // processNextLlmBatch
    // ──────────────────────────────────────────────

    @Test
    void processNextLlmBatch_정상_분석_완료() {
        NewsArticle article = NewsArticle.builder()
                .stock(stock).source("Reuters")
                .sourceUrl("https://r.co/llm1")
                .headline("Apple revenue beats forecast")
                .publishedAt(LocalDateTime.now())
                .build();

        when(newsArticleRepository.findByLlmProcessedFalseOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(article)));
        when(llmClient.analyze(any(), any()))
                .thenReturn(new LLMAnalysis("애플 매출 예상 상회.", new BigDecimal("0.65"), 78, "HIGH"));
        when(newsArticleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processNextLlmBatch(10);

        verify(llmClient).analyze(eq("Apple revenue beats forecast"), isNull());
        verify(newsArticleRepository).save(any(NewsArticle.class));
        assertThat(article.getLlmProcessed()).isTrue();
        assertThat(article.getSentimentScore()).isEqualByComparingTo(new BigDecimal("0.65"));
    }

    @Test
    void processNextLlmBatch_rate_limit_시_즉시_중단() {
        NewsArticle a1 = articleStub("https://r.co/a1", "Apple earnings");
        NewsArticle a2 = articleStub("https://r.co/a2", "Apple revenue guidance");

        when(newsArticleRepository.findByLlmProcessedFalseOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a1, a2)));
        when(llmClient.analyze(any(), any()))
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_CIRCUIT_OPEN));

        boolean hasMore = service.processNextLlmBatch(10);

        // Rate limit 발생 시 break — a2는 처리 안 됨
        verify(llmClient, times(1)).analyze(any(), any());
        assertThat(hasMore).isTrue(); // 미처리 항목 남아있음
    }

    @Test
    void processNextLlmBatch_일시적_오류_시_다음_항목_계속() {
        NewsArticle a1 = articleStub("https://r.co/fail", "Apple acquisition deal");
        NewsArticle a2 = articleStub("https://r.co/ok",   "Apple annual earnings beat");

        when(newsArticleRepository.findByLlmProcessedFalseOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a1, a2)));
        when(llmClient.analyze(eq("Apple acquisition deal"), any()))
                .thenThrow(new BusinessException(ErrorCode.EXTERNAL_API_ERROR));
        when(llmClient.analyze(eq("Apple annual earnings beat"), any()))
                .thenReturn(new LLMAnalysis("애플 연간 실적 호조.", new BigDecimal("0.5"), 70, "HIGH"));
        when(newsArticleRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processNextLlmBatch(10);

        // 두 번 모두 호출됨 — rate limit이 아닌 오류는 중단하지 않음
        verify(llmClient, times(2)).analyze(any(), any());
    }

    @Test
    void processNextLlmBatch_미처리_없으면_false() {
        when(newsArticleRepository.findByLlmProcessedFalseOrderByCreatedAtAsc(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        boolean hasMore = service.processNextLlmBatch(10);

        assertThat(hasMore).isFalse();
        verifyNoInteractions(llmClient);
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private RawNews raw(String headline, String url, String snippet) {
        return new RawNews(headline, "Reuters", url, LocalDateTime.now(), snippet);
    }

    private NewsArticle articleStub(String url, String headline) {
        return NewsArticle.builder()
                .stock(stock).source("Reuters")
                .sourceUrl(url).headline(headline)
                .publishedAt(LocalDateTime.now())
                .build();
    }
}
