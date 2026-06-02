package com.stocknews.api.domain.financial;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FinancialMetricRepository extends JpaRepository<FinancialMetric, Long> {

    // GET /stocks/{ticker}/financials?period=annual|quarterly&limit=4
    List<FinancialMetric> findByStockIdAndPeriodOrderByFiscalDateDesc(Long stockId, String period);

    // 스크리닝: 특정 종목의 최신 지표 1개
    Optional<FinancialMetric> findTopByStockIdAndPeriodOrderByFiscalDateDesc(Long stockId, String period);

    // 스크리닝: 성장 트렌드 계산용 최근 3개 분기 지표
    List<FinancialMetric> findTop3ByStockIdAndPeriodOrderByFiscalDateDesc(Long stockId, String period);

    // 스케줄러: 해당 분기 이미 수집됐는지 확인 (중복 방지)
    boolean existsByStockIdAndPeriodAndFiscalDate(Long stockId, String period, LocalDate fiscalDate);

    // 섹터 랭킹: 섹터 내 종목별 최신 분기 지표의 평균 매출성장률 (±500% 이상 이상치 제외)
    @Query("""
            SELECT AVG(fm.revenueGrowthYoy)
            FROM FinancialMetric fm
            WHERE fm.stock.sector.id = :sectorId
              AND fm.period = 'quarterly'
              AND fm.revenueGrowthYoy IS NOT NULL
              AND ABS(fm.revenueGrowthYoy) <= 500
              AND fm.fiscalDate = (
                  SELECT MAX(fm2.fiscalDate) FROM FinancialMetric fm2
                  WHERE fm2.stock.id = fm.stock.id AND fm2.period = 'quarterly'
              )
            """)
    Optional<BigDecimal> avgLatestRevenueGrowthBySector(@Param("sectorId") Long sectorId);

    // 섹터 랭킹 모멘텀: 종목의 최근 2개 분기 지표 (영업이익률 추이 비교용)
    List<FinancialMetric> findTop2ByStockIdAndPeriodOrderByFiscalDateDesc(Long stockId, String period);

    // GET /sectors/{id}/rule-of-40, /valuation — 섹터 내 종목별 최신 분기 지표 일괄 조회
    @Query("""
            SELECT fm FROM FinancialMetric fm
            JOIN FETCH fm.stock s
            WHERE s.sector.id = :sectorId
              AND fm.period = 'quarterly'
              AND fm.fiscalDate = (
                  SELECT MAX(fm2.fiscalDate) FROM FinancialMetric fm2
                  WHERE fm2.stock.id = fm.stock.id AND fm2.period = 'quarterly'
              )
            """)
    List<FinancialMetric> findLatestQuarterlyBySector(@Param("sectorId") Long sectorId);

    // 스케줄러: 특정 분기 지표가 없는 종목 ID 목록 조회 (미수집 종목만 처리용)
    @Query("""
            SELECT st FROM Stock st
            WHERE st.market = :market
              AND NOT EXISTS (
                  SELECT 1 FROM FinancialMetric fm
                  WHERE fm.stock.id = st.id
                    AND fm.period = :period
                    AND fm.fiscalDate = :fiscalDate
              )
            ORDER BY st.id
            """)
    List<com.stocknews.api.domain.stock.Stock> findStocksWithoutMetrics(
            @Param("market") String market,
            @Param("period") String period,
            @Param("fiscalDate") LocalDate fiscalDate);
}
