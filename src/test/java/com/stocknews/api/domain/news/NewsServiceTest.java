package com.stocknews.api.domain.news;

import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.domain.news.dto.NewsPageResponse;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import com.stocknews.api.domain.stock.dto.StockSummaryResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NewsServiceTest {

    @Mock NewsArticleRepository newsArticleRepository;
    @Mock StockRepository stockRepository;

    NewsService newsService;
    Stock stock;

    @BeforeEach
    void setUp() {
        newsService = new NewsService(newsArticleRepository, stockRepository);
        stock = Stock.builder().ticker("AAPL").market("US").companyName("Apple Inc.").build();
    }

    // ──────────────────────────────────────────────
    // getNews
    // ──────────────────────────────────────────────

    @Test
    void getNews_정상_목록_반환() {
        NewsArticle a = articleWithLlm("https://r.co/1", "Apple earnings beat",
                "애플 1분기 실적 호조.", new BigDecimal("0.7"), 80, "HIGH");

        when(stockRepository.findByMarketAndTicker("US", "AAPL")).thenReturn(Optional.of(stock));
        when(newsArticleRepository.searchByFilters(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(a)));

        NewsPageResponse response = newsService.getNews("AAPL", 0, 20, null, null, null);

        assertThat(response.ticker()).isEqualTo("AAPL");
        assertThat(response.news()).hasSize(1);
        assertThat(response.news().get(0).headline()).isEqualTo("Apple earnings beat");
        assertThat(response.news().get(0).sentiment()).isEqualByComparingTo(new BigDecimal("0.7"));
        assertThat(response.news().get(0).importance()).isEqualTo(80);
    }

    @Test
    void getNews_종목_없으면_예외() {
        when(stockRepository.findByMarketAndTicker("US", "UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.getNews("UNKNOWN", 0, 20, null, null, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getNews_LLM_미처리_기사_null_필드로_포함() {
        NewsArticle unprocessed = NewsArticle.builder()
                .stock(stock).source("Reuters")
                .sourceUrl("https://r.co/raw").headline("Apple store news")
                .publishedAt(LocalDateTime.now())
                .build(); // llmProcessed=false, summary/sentiment/importance/relevance=null

        when(stockRepository.findByMarketAndTicker("US", "AAPL")).thenReturn(Optional.of(stock));
        when(newsArticleRepository.searchByFilters(any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unprocessed)));

        NewsPageResponse response = newsService.getNews("AAPL", 0, 20, null, null, null);

        assertThat(response.news().get(0).summary()).isNull();
        assertThat(response.news().get(0).sentiment()).isNull();
    }

    // ──────────────────────────────────────────────
    // getSummary
    // ──────────────────────────────────────────────

    @Test
    void getSummary_긍정_감정_요약_생성() {
        NewsArticle a1 = articleWithLlm("https://r.co/s1", "h1", "s1", new BigDecimal("0.8"),  90, "HIGH");
        NewsArticle a2 = articleWithLlm("https://r.co/s2", "h2", "s2", new BigDecimal("0.6"),  70, "HIGH");

        when(stockRepository.findByMarketAndTicker("US", "AAPL")).thenReturn(Optional.of(stock));
        when(newsArticleRepository.findTop20ByStockIdAndLlmProcessedTrueOrderByPublishedAtDesc(any()))
                .thenReturn(List.of(a1, a2));

        StockSummaryResponse summary = newsService.getSummary("AAPL");

        assertThat(summary.ticker()).isEqualTo("AAPL");
        assertThat(summary.analyzedNewsCount()).isEqualTo(2);
        assertThat(summary.avgSentiment()).isEqualByComparingTo(new BigDecimal("0.7000")); // (0.8+0.6)/2
        assertThat(summary.summaryComment()).contains("긍정적");
    }

    @Test
    void getSummary_뉴스_없으면_기본_메시지() {
        when(stockRepository.findByMarketAndTicker("US", "AAPL")).thenReturn(Optional.of(stock));
        when(newsArticleRepository.findTop20ByStockIdAndLlmProcessedTrueOrderByPublishedAtDesc(any()))
                .thenReturn(List.of());

        StockSummaryResponse summary = newsService.getSummary("AAPL");

        assertThat(summary.analyzedNewsCount()).isZero();
        assertThat(summary.summaryComment()).contains("분석된 뉴스가 없습니다");
        assertThat(summary.avgSentiment()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getSummary_종목_없으면_예외() {
        when(stockRepository.findByMarketAndTicker("US", "UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> newsService.getSummary("UNKNOWN"))
                .isInstanceOf(BusinessException.class);
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private NewsArticle articleWithLlm(String url, String headline, String summary,
                                        BigDecimal sentiment, int importance, String relevance) {
        NewsArticle article = NewsArticle.builder()
                .stock(stock).source("Reuters").sourceUrl(url)
                .headline(headline).publishedAt(LocalDateTime.now())
                .build();
        article.applyLlmAnalysis(summary, sentiment, importance, relevance);
        return article;
    }
}
