package com.stocknews.api.domain.news;

import com.stocknews.api.domain.sector.Sector;
import com.stocknews.api.domain.sector.SectorRepository;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import com.stocknews.api.support.RepositoryTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NewsArticleRepositoryTest extends RepositoryTestSupport {

    @Autowired
    NewsArticleRepository newsArticleRepository;

    @Autowired
    StockRepository stockRepository;

    @Autowired
    SectorRepository sectorRepository;

    @Autowired
    EntityManager em;

    private Stock stock;

    @BeforeEach
    void setUp() {
        Sector sector = sectorRepository.saveAndFlush(
                Sector.builder().code("AI_SOFTWARE").name("AI/소프트웨어").build()
        );
        stock = stockRepository.saveAndFlush(
                Stock.builder().ticker("MSFT").market("US").companyName("Microsoft").sector(sector).build()
        );
    }

    @Test
    void 저장_후_기본정보_조회() {
        NewsArticle article = NewsArticle.builder()
                .stock(stock)
                .source("Reuters")
                .sourceUrl("https://reuters.com/news/msft-earnings-2024")
                .headline("Microsoft Q4 Earnings Beat Expectations")
                .publishedAt(LocalDateTime.of(2024, 7, 30, 18, 0))
                .build();

        NewsArticle saved = newsArticleRepository.saveAndFlush(article);
        em.clear();

        NewsArticle found = newsArticleRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getSource()).isEqualTo("Reuters");
        assertThat(found.getHeadline()).isEqualTo("Microsoft Q4 Earnings Beat Expectations");
        assertThat(found.getLlmProcessed()).isFalse();
        assertThat(found.getSummary()).isNull();     // LLM 미처리 상태
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void source_url_유니크_제약_중복_저장시_예외() {
        String url = "https://reuters.com/news/duplicate";
        newsArticleRepository.saveAndFlush(
                NewsArticle.builder().stock(stock).source("Reuters")
                        .sourceUrl(url).headline("원본").build()
        );

        assertThatThrownBy(() ->
                newsArticleRepository.saveAndFlush(
                        NewsArticle.builder().stock(stock).source("Bloomberg")
                                .sourceUrl(url).headline("중복").build()
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void existsBySourceUrl_존재하는_경우() {
        String url = "https://reuters.com/news/exists";
        newsArticleRepository.saveAndFlush(
                NewsArticle.builder().stock(stock).source("Reuters")
                        .sourceUrl(url).headline("테스트").build()
        );

        assertThat(newsArticleRepository.existsBySourceUrl(url)).isTrue();
    }

    @Test
    void existsBySourceUrl_없는_경우() {
        assertThat(newsArticleRepository.existsBySourceUrl("https://notexist.com")).isFalse();
    }

    @Test
    void applyLlmAnalysis_분석결과_저장_후_조회() {
        NewsArticle article = newsArticleRepository.saveAndFlush(
                NewsArticle.builder().stock(stock).source("WSJ")
                        .sourceUrl("https://wsj.com/news/llm-test")
                        .headline("LLM 분석 테스트").build()
        );

        article.applyLlmAnalysis(
                "마이크로소프트의 클라우드 매출이 전년 대비 29% 성장했습니다.",
                new BigDecimal("0.72"),
                85,
                "HIGH"
        );
        newsArticleRepository.flush();
        em.clear();

        NewsArticle updated = newsArticleRepository.findById(article.getId()).orElseThrow();
        assertThat(updated.getLlmProcessed()).isTrue();
        assertThat(updated.getSummary()).contains("클라우드 매출");
        assertThat(updated.getSentimentScore()).isEqualByComparingTo(new BigDecimal("0.72"));
        assertThat(updated.getImportanceScore()).isEqualTo(85);
        assertThat(updated.getRelevance()).isEqualTo("HIGH");
    }
}
