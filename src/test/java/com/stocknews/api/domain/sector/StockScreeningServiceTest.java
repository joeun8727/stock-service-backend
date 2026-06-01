package com.stocknews.api.domain.sector;

import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.domain.financial.FinancialMetric;
import com.stocknews.api.domain.financial.FinancialMetricRepository;
import com.stocknews.api.domain.sector.dto.SectorStocksResponse;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StockScreeningServiceTest {

    @Mock SectorRepository sectorRepository;
    @Mock StockRepository stockRepository;
    @Mock FinancialMetricRepository financialMetricRepository;

    StockScreeningService service;
    Sector sector;
    Stock stockA;
    Stock stockB;

    @BeforeEach
    void setUp() {
        service = new StockScreeningService(sectorRepository, stockRepository, financialMetricRepository);
        // 가중치 및 시총 상한 주입 (application.yml 대신 직접 설정)
        ReflectionTestUtils.setField(service, "weightRevenueGrowth",       0.40);
        ReflectionTestUtils.setField(service, "weightRoicTrend",           0.25);
        ReflectionTestUtils.setField(service, "weightOperatingMarginTrend",0.20);
        ReflectionTestUtils.setField(service, "weightMarketCapPotential",  0.15);
        ReflectionTestUtils.setField(service, "maxGrowthMarketCap",        new BigDecimal("500000000000"));

        sector = Sector.builder().code("SEMICONDUCTOR").name("반도체").build();
        ReflectionTestUtils.setField(sector, "id", 1L);

        stockA = Stock.builder().ticker("AAPL").market("US").companyName("Apple Inc.")
                .marketCap(new BigDecimal("100000000000")).build();
        ReflectionTestUtils.setField(stockA, "id", 10L);

        stockB = Stock.builder().ticker("TSMC").market("US").companyName("TSMC")
                .marketCap(new BigDecimal("200000000000")).build();
        ReflectionTestUtils.setField(stockB, "id", 11L);
    }

    // ──────────────────────────────────────────────
    // getLargeCapStocks
    // ──────────────────────────────────────────────

    @Test
    void getLargeCapStocks_시가총액_내림차순_Top2() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        // stockB(200B) → stockA(100B) 순으로 반환됨을 가정
        when(stockRepository.findBySectorIdAndMarketOrderByMarketCapDesc(1L, "US"))
                .thenReturn(List.of(stockB, stockA));
        when(financialMetricRepository.findTopByStockIdAndPeriodOrderByFiscalDateDesc(anyLong(), eq("quarterly")))
                .thenAnswer(inv -> {
                    Long stockId = inv.getArgument(0);
                    return Optional.of(metricWith(stockId.equals(10L) ? stockA : stockB,
                            new BigDecimal("0.15"), new BigDecimal("0.30")));
                });

        SectorStocksResponse result = service.getLargeCapStocks(1L, 2);

        assertThat(result.type()).isEqualTo("large_cap");
        assertThat(result.stocks()).hasSize(2);
        assertThat(result.stocks().get(0).rank()).isEqualTo(1);
        assertThat(result.stocks().get(0).ticker()).isEqualTo("TSMC");
        assertThat(result.stocks().get(0).growthScore()).isNull(); // 대형주는 성장 점수 없음
    }

    @Test
    void getLargeCapStocks_재무지표_없는_종목_제외() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(stockRepository.findBySectorIdAndMarketOrderByMarketCapDesc(1L, "US"))
                .thenReturn(List.of(stockB, stockA));
        // stockB는 재무지표 없음
        when(financialMetricRepository.findTopByStockIdAndPeriodOrderByFiscalDateDesc(eq(11L), any()))
                .thenReturn(Optional.empty());
        when(financialMetricRepository.findTopByStockIdAndPeriodOrderByFiscalDateDesc(eq(10L), any()))
                .thenReturn(Optional.of(metricWith(stockA, new BigDecimal("0.10"), new BigDecimal("0.15"))));

        SectorStocksResponse result = service.getLargeCapStocks(1L, 20);

        assertThat(result.stocks()).hasSize(1);
        assertThat(result.stocks().get(0).ticker()).isEqualTo("AAPL");
    }

    @Test
    void getLargeCapStocks_섹터_없으면_예외() {
        when(sectorRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getLargeCapStocks(999L, 20))
                .isInstanceOf(BusinessException.class);
    }

    // ──────────────────────────────────────────────
    // getGrowthStocks
    // ──────────────────────────────────────────────

    @Test
    void getGrowthStocks_성장_점수_높은_종목이_상위() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(stockRepository.findGrowthCandidatesBySector(eq(1L), eq("US"), any()))
                .thenReturn(List.of(stockA, stockB));

        // stockA: 매출성장률 50% (높음), stockB: 매출성장률 10% (낮음)
        when(financialMetricRepository.findTop3ByStockIdAndPeriodOrderByFiscalDateDesc(eq(10L), any()))
                .thenReturn(List.of(metricWithRevenue(stockA, new BigDecimal("0.50"))));
        when(financialMetricRepository.findTop3ByStockIdAndPeriodOrderByFiscalDateDesc(eq(11L), any()))
                .thenReturn(List.of(metricWithRevenue(stockB, new BigDecimal("0.10"))));

        SectorStocksResponse result = service.getGrowthStocks(1L, 20);

        assertThat(result.type()).isEqualTo("growth");
        assertThat(result.stocks()).hasSize(2);
        // stockA(50% 성장)가 1위
        assertThat(result.stocks().get(0).ticker()).isEqualTo("AAPL");
        assertThat(result.stocks().get(0).growthScore()).isNotNull();
    }

    @Test
    void getGrowthStocks_매출성장률_없는_종목_제외() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(stockRepository.findGrowthCandidatesBySector(eq(1L), eq("US"), any()))
                .thenReturn(List.of(stockA, stockB));

        // stockA: 매출성장률 없음 → 제외
        FinancialMetric noRevenue = FinancialMetric.builder()
                .stock(stockA).period("quarterly").fiscalDate(LocalDate.now())
                .roic(new BigDecimal("0.10")).operatingMargin(new BigDecimal("0.15"))
                .build();
        when(financialMetricRepository.findTop3ByStockIdAndPeriodOrderByFiscalDateDesc(eq(10L), any()))
                .thenReturn(List.of(noRevenue));
        when(financialMetricRepository.findTop3ByStockIdAndPeriodOrderByFiscalDateDesc(eq(11L), any()))
                .thenReturn(List.of(metricWithRevenue(stockB, new BigDecimal("0.30"))));

        SectorStocksResponse result = service.getGrowthStocks(1L, 20);

        assertThat(result.stocks()).hasSize(1);
        assertThat(result.stocks().get(0).ticker()).isEqualTo("TSMC");
    }

    @Test
    void getGrowthStocks_재무지표_없는_종목_제외() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(stockRepository.findGrowthCandidatesBySector(eq(1L), eq("US"), any()))
                .thenReturn(List.of(stockA));
        when(financialMetricRepository.findTop3ByStockIdAndPeriodOrderByFiscalDateDesc(anyLong(), any()))
                .thenReturn(List.of()); // 재무지표 없음

        SectorStocksResponse result = service.getGrowthStocks(1L, 20);

        assertThat(result.stocks()).isEmpty();
    }

    @Test
    void getGrowthStocks_limit_적용() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(stockRepository.findGrowthCandidatesBySector(eq(1L), eq("US"), any()))
                .thenReturn(List.of(stockA, stockB));
        when(financialMetricRepository.findTop3ByStockIdAndPeriodOrderByFiscalDateDesc(anyLong(), any()))
                .thenAnswer(inv -> {
                    Long stockId = inv.getArgument(0);
                    Stock s = stockId.equals(10L) ? stockA : stockB;
                    return List.of(metricWithRevenue(s, new BigDecimal("0.20")));
                });

        SectorStocksResponse result = service.getGrowthStocks(1L, 1);

        assertThat(result.stocks()).hasSize(1);
    }

    @Test
    void getGrowthStocks_3분기_ROIC_개선_추이_점수_반영() {
        when(sectorRepository.findById(1L)).thenReturn(Optional.of(sector));
        when(stockRepository.findGrowthCandidatesBySector(eq(1L), eq("US"), any()))
                .thenReturn(List.of(stockA, stockB));

        // stockA: ROIC 개선 추이 있음 (Q3=0.20 > Q2=0.15 > Q1=0.10)
        FinancialMetric a_q3 = metricWithRoic(stockA, new BigDecimal("0.50"), new BigDecimal("0.20"));
        FinancialMetric a_q2 = metricWithRoic(stockA, new BigDecimal("0.50"), new BigDecimal("0.15"));
        FinancialMetric a_q1 = metricWithRoic(stockA, new BigDecimal("0.50"), new BigDecimal("0.10"));
        when(financialMetricRepository.findTop3ByStockIdAndPeriodOrderByFiscalDateDesc(eq(10L), any()))
                .thenReturn(List.of(a_q3, a_q2, a_q1));

        // stockB: ROIC 개선 없음 (단일 데이터)
        when(financialMetricRepository.findTop3ByStockIdAndPeriodOrderByFiscalDateDesc(eq(11L), any()))
                .thenReturn(List.of(metricWithRoic(stockB, new BigDecimal("0.50"), new BigDecimal("0.10"))));

        SectorStocksResponse result = service.getGrowthStocks(1L, 20);

        // stockA와 stockB 모두 같은 매출성장률이지만, stockA가 ROIC 트렌드 점수 우위
        assertThat(result.stocks().get(0).ticker()).isEqualTo("AAPL");
        assertThat(result.stocks().get(0).growthScore())
                .isGreaterThan(result.stocks().get(1).growthScore());
    }

    // ──────────────────────────────────────────────
    // 헬퍼
    // ──────────────────────────────────────────────

    private FinancialMetric metricWith(Stock stock, BigDecimal roic, BigDecimal operatingMargin) {
        return FinancialMetric.builder()
                .stock(stock).period("quarterly").fiscalDate(LocalDate.now())
                .revenueGrowthYoy(new BigDecimal("0.20"))
                .roic(roic).operatingMargin(operatingMargin)
                .build();
    }

    private FinancialMetric metricWithRevenue(Stock stock, BigDecimal revenueGrowthYoy) {
        return FinancialMetric.builder()
                .stock(stock).period("quarterly").fiscalDate(LocalDate.now())
                .revenueGrowthYoy(revenueGrowthYoy)
                .roic(new BigDecimal("0.10")).operatingMargin(new BigDecimal("0.15"))
                .build();
    }

    private FinancialMetric metricWithRoic(Stock stock, BigDecimal revenueGrowthYoy, BigDecimal roic) {
        return FinancialMetric.builder()
                .stock(stock).period("quarterly").fiscalDate(LocalDate.now())
                .revenueGrowthYoy(revenueGrowthYoy).roic(roic)
                .operatingMargin(new BigDecimal("0.15"))
                .build();
    }
}
