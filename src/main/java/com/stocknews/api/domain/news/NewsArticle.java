package com.stocknews.api.domain.news;

import com.stocknews.api.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "news_article",
    uniqueConstraints = @UniqueConstraint(columnNames = "source_url")
)
@Getter
@NoArgsConstructor
public class NewsArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "source_url", nullable = false, length = 500)
    private String sourceUrl;

    @Column(nullable = false, length = 500)
    private String headline;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    // 원문 본문 저장 절대 금지 (08-compliance.md) — LLM 요약만 저장
    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "sentiment_score", precision = 3, scale = 2)
    private BigDecimal sentimentScore;

    @Column(name = "importance_score")
    private Integer importanceScore;

    @Column(length = 10)
    private String relevance;

    @Column(name = "llm_processed")
    private Boolean llmProcessed = false;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public NewsArticle(Stock stock, String source, String sourceUrl, String headline,
                       LocalDateTime publishedAt) {
        this.stock = stock;
        this.source = source;
        this.sourceUrl = sourceUrl;
        this.headline = headline;
        this.publishedAt = publishedAt;
        this.llmProcessed = false;
        this.createdAt = LocalDateTime.now();
    }

    public void applyLlmAnalysis(String summary, BigDecimal sentimentScore,
                                  int importanceScore, String relevance) {
        this.summary = summary;
        this.sentimentScore = sentimentScore;
        this.importanceScore = importanceScore;
        this.relevance = relevance;
        this.llmProcessed = true;
    }
}
