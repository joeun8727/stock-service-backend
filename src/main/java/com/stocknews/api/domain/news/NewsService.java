package com.stocknews.api.domain.news;

import com.stocknews.api.common.config.CacheKeys;
import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.domain.news.dto.NewsPageResponse;
import com.stocknews.api.domain.news.dto.NewsPageResponse.NewsItemResponse;
import com.stocknews.api.domain.news.dto.TopNewsResponse;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import com.stocknews.api.domain.stock.dto.SentimentTrendResponse;
import com.stocknews.api.domain.stock.dto.StockSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NewsService {

    private final NewsArticleRepository newsArticleRepository;
    private final StockRepository stockRepository;

    // ──────────────────────────────────────────────
    // GET /api/v1/stocks/{ticker}/news
    // ──────────────────────────────────────────────

    public NewsPageResponse getNews(String ticker, int page, int size,
                                    LocalDate from, LocalDate to, Integer minImportance) {
        String upper = ticker.toUpperCase();
        Stock stock = stockRepository.findByMarketAndTicker("US", upper)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND, upper));

        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.atTime(23, 59, 59) : null;

        Page<NewsArticle> page_ = newsArticleRepository.searchByFilters(
                stock.getId(), fromDt, toDt, minImportance,
                PageRequest.of(page, size)
        );

        List<NewsItemResponse> items = page_.getContent().stream()
                .map(this::toItemResponse)
                .toList();

        return new NewsPageResponse(upper, items, page_.getTotalElements(),
                page_.getTotalPages(), page);
    }

    // ──────────────────────────────────────────────
    // GET /api/v1/stocks/{ticker}/summary (캐시 6시간)
    // ──────────────────────────────────────────────

    @Cacheable(value = CacheKeys.STOCK_SUMMARY, key = "#ticker.toUpperCase()")
    public StockSummaryResponse getSummary(String ticker) {
        String upper = ticker.toUpperCase();
        Stock stock = stockRepository.findByMarketAndTicker("US", upper)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND, upper));

        List<NewsArticle> recent =
                newsArticleRepository.findTop20ByStockIdAndLlmProcessedTrueOrderByPublishedAtDesc(stock.getId());

        if (recent.isEmpty()) {
            return new StockSummaryResponse(upper, stock.getCompanyName(),
                    "최근 분석된 뉴스가 없습니다.", BigDecimal.ZERO, 0);
        }

        BigDecimal sum = recent.stream()
                .map(NewsArticle::getSentimentScore)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long validCount = recent.stream()
                .map(NewsArticle::getSentimentScore)
                .filter(Objects::nonNull)
                .count();

        BigDecimal avgSentiment = validCount > 0
                ? sum.divide(BigDecimal.valueOf(validCount), 4, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        String label = avgSentiment.compareTo(new BigDecimal("0.3")) > 0  ? "긍정적" :
                       avgSentiment.compareTo(new BigDecimal("-0.3")) < 0 ? "부정적" : "중립적";
        String comment = String.format("최근 %d개 뉴스 분석 기준 평균 감정은 %s(%.2f)입니다.",
                recent.size(), label, avgSentiment);

        return new StockSummaryResponse(upper, stock.getCompanyName(),
                comment, avgSentiment, recent.size());
    }

    // ──────────────────────────────────────────────
    // GET /api/v1/stocks/{ticker}/sentiment-trend (캐시 1시간)
    // ──────────────────────────────────────────────

    private static final int SENTIMENT_TREND_DAYS = 30;

    @Cacheable(value = CacheKeys.STOCK_SENTIMENT_TREND, key = "#ticker.toUpperCase()")
    public SentimentTrendResponse getSentimentTrend(String ticker) {
        String upper = ticker.toUpperCase();
        Stock stock = stockRepository.findByMarketAndTicker("US", upper)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND, upper));

        LocalDateTime from = LocalDateTime.now().minusDays(SENTIMENT_TREND_DAYS);
        List<NewsArticleRepository.DailyNewsStat> raw =
                newsArticleRepository.dailyStatsByStock(stock.getId(), from);

        List<SentimentTrendResponse.DailySentiment> trend = raw.stream()
                .map(r -> new SentimentTrendResponse.DailySentiment(
                        r.getStatDate(),
                        r.getNewsCount(),
                        r.getAvgSentiment() != null
                                ? r.getAvgSentiment().setScale(4, RoundingMode.HALF_UP)
                                : null
                ))
                .toList();

        return new SentimentTrendResponse(upper, stock.getCompanyName(), SENTIMENT_TREND_DAYS, trend);
    }

    // ──────────────────────────────────────────────
    // GET /api/v1/news/top (캐시 30분)
    // ──────────────────────────────────────────────

    @Cacheable(value = CacheKeys.TOP_NEWS, key = "(#minImportance == null ? 0 : #minImportance) + '_' + #size")
    public TopNewsResponse getTopNews(Integer minImportance, int size) {
        List<NewsArticle> articles = newsArticleRepository.findTopNewsByImportance(
                minImportance, PageRequest.of(0, size));

        List<TopNewsResponse.TopNewsItem> items = articles.stream()
                .map(a -> new TopNewsResponse.TopNewsItem(
                        a.getStock().getTicker(),
                        a.getStock().getCompanyName(),
                        a.getHeadline(),
                        a.getSource(),
                        a.getSourceUrl(),
                        a.getPublishedAt(),
                        a.getSummary(),
                        a.getSentimentScore(),
                        a.getImportanceScore(),
                        a.getRelevance()
                ))
                .toList();

        return new TopNewsResponse(items, items.size());
    }

    // ──────────────────────────────────────────────
    // 변환 헬퍼
    // ──────────────────────────────────────────────

    private NewsItemResponse toItemResponse(NewsArticle a) {
        return new NewsItemResponse(
                a.getHeadline(),
                a.getSource(),
                a.getSourceUrl(),
                a.getPublishedAt(),
                a.getSummary(),
                a.getSentimentScore(),
                a.getImportanceScore(),
                a.getRelevance()
        );
    }
}
