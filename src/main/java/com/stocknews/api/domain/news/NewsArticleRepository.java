package com.stocknews.api.domain.news;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface NewsArticleRepository extends JpaRepository<NewsArticle, Long> {

    boolean existsBySourceUrl(String sourceUrl);

    Optional<NewsArticle> findBySourceUrl(String sourceUrl);

    // GET /stocks/{ticker}/news — 다중 필터 + 페이징
    @Query("""
            SELECT n FROM NewsArticle n
            WHERE n.stock.id = :stockId
              AND (:from IS NULL OR n.publishedAt >= :from)
              AND (:to IS NULL OR n.publishedAt <= :to)
              AND (:minImportance IS NULL OR n.importanceScore >= :minImportance)
            ORDER BY n.publishedAt DESC
            """)
    Page<NewsArticle> searchByFilters(@Param("stockId") Long stockId,
                                      @Param("from") LocalDateTime from,
                                      @Param("to") LocalDateTime to,
                                      @Param("minImportance") Integer minImportance,
                                      Pageable pageable);

    // 스케줄러: LLM 미처리 기사 배치 조회 (createdAt 오름차순 — 오래된 것 먼저)
    Page<NewsArticle> findByLlmProcessedFalseOrderByCreatedAtAsc(Pageable pageable);

    // GET /stocks/{ticker}/summary — 최근 LLM 처리 완료 기사 20건
    List<NewsArticle> findTop20ByStockIdAndLlmProcessedTrueOrderByPublishedAtDesc(Long stockId);

    // 섹터 랭킹: 기간별 섹터 뉴스 수 (뉴스 볼륨 추이)
    @Query("SELECT COUNT(n) FROM NewsArticle n WHERE n.stock.sector.id = :sectorId AND n.publishedAt BETWEEN :from AND :to")
    long countBySectorAndPeriod(@Param("sectorId") Long sectorId,
                                @Param("from") LocalDateTime from,
                                @Param("to") LocalDateTime to);

    // 섹터 랭킹: 기간별 평균 감정 점수 (LLM 처리 완료 기사만)
    @Query("SELECT AVG(n.sentimentScore) FROM NewsArticle n WHERE n.stock.sector.id = :sectorId AND n.llmProcessed = true AND n.publishedAt BETWEEN :from AND :to")
    BigDecimal avgSentimentBySectorAndPeriod(@Param("sectorId") Long sectorId,
                                              @Param("from") LocalDateTime from,
                                              @Param("to") LocalDateTime to);

    // 섹터 트렌드: 일별 뉴스 수 + 평균 감정 (최근 N일)
    @Query(value = """
            SELECT DATE(n.published_at) AS stat_date,
                   COUNT(*) AS news_count,
                   AVG(n.sentiment_score) AS avg_sentiment
            FROM news_article n
            JOIN stock s ON n.stock_id = s.id
            WHERE s.sector_id = :sectorId AND n.published_at >= :from
            GROUP BY DATE(n.published_at)
            ORDER BY stat_date ASC
            """, nativeQuery = true)
    List<DailyNewsStat> dailyStatsBySector(@Param("sectorId") Long sectorId,
                                           @Param("from") LocalDateTime from);

    // GET /stocks/{ticker}/sentiment-trend — 종목별 일별 감정 추이 (최근 N일)
    @Query(value = """
            SELECT DATE(n.published_at) AS stat_date,
                   COUNT(*) AS news_count,
                   AVG(n.sentiment_score) AS avg_sentiment
            FROM news_article n
            WHERE n.stock_id = :stockId
              AND n.llm_processed = true
              AND n.published_at >= :from
            GROUP BY DATE(n.published_at)
            ORDER BY stat_date ASC
            """, nativeQuery = true)
    List<DailyNewsStat> dailyStatsByStock(@Param("stockId") Long stockId,
                                          @Param("from") LocalDateTime from);

    // GET /news/top — 전체 종목 횡단 중요 뉴스 피드 (importance 필터, 최신순)
    @Query("""
            SELECT n FROM NewsArticle n
            JOIN FETCH n.stock s
            WHERE n.llmProcessed = true
              AND (:minImportance IS NULL OR n.importanceScore >= :minImportance)
            ORDER BY n.publishedAt DESC
            """)
    List<NewsArticle> findTopNewsByImportance(@Param("minImportance") Integer minImportance,
                                              Pageable pageable);

    // 일별 통계 프로젝션 — MySQL DATE() → LocalDate (JDBC 드라이버가 직접 매핑)
    interface DailyNewsStat {
        LocalDate getStatDate();
        long getNewsCount();
        BigDecimal getAvgSentiment();
    }
}
