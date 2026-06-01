package com.stocknews.api.domain.stock;

import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.domain.financial.FinancialMetric;
import com.stocknews.api.domain.financial.FinancialMetricRepository;
import com.stocknews.api.domain.sector.Sector;
import com.stocknews.api.domain.stock.dto.StockProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock StockRepository stockRepository;
    @Mock FinancialMetricRepository financialMetricRepository;

    StockService service;
    Stock stock;
    Sector sector;

    @BeforeEach
    void setUp() {
        service = new StockService(stockRepository, financialMetricRepository);

        sector = Sector.builder().code("SEMICONDUCTOR").name("반도체").build();
        stock = Stock.builder()
                .ticker("AAPL").market("US").companyName("Apple Inc.")
                .sector(sector).industry("Technology Hardware")
                .marketCap(new BigDecimal("3000000000000"))
                .exchange("NASDAQ").website("https://apple.com")
                .ipoDate(LocalDate.of(1980, 12, 12))
                .build();
        ReflectionTestUtils.setField(stock, "id", 1L);
    }

    @Test
    void getStockProfile_재무지표_포함_정상_반환() {
        FinancialMetric metric = FinancialMetric.builder()
                .stock(stock).period("quarterly").fiscalDate(LocalDate.now())
                .roe(new BigDecimal("1.5632")).roa(new BigDecimal("0.2891"))
                .roic(new BigDecimal("0.4512")).per(new BigDecimal("28.34"))
                .pbr(new BigDecimal("45.12")).eps(new BigDecimal("6.43"))
                .debtRatio(new BigDecimal("1.72")).revenueGrowthYoy(new BigDecimal("0.08"))
                .operatingMargin(new BigDecimal("0.31"))
                .build();

        when(stockRepository.findByMarketAndTicker("US", "AAPL")).thenReturn(Optional.of(stock));
        when(financialMetricRepository.findTopByStockIdAndPeriodOrderByFiscalDateDesc(eq(1L), eq("quarterly")))
                .thenReturn(Optional.of(metric));

        StockProfileResponse response = service.getStockProfile("aapl"); // 소문자 입력

        assertThat(response.ticker()).isEqualTo("AAPL");
        assertThat(response.companyName()).isEqualTo("Apple Inc.");
        assertThat(response.sector()).isEqualTo("반도체");
        assertThat(response.latestMetrics()).isNotNull();
        assertThat(response.latestMetrics().roe()).isEqualByComparingTo(new BigDecimal("1.5632"));
        assertThat(response.latestMetrics().per()).isEqualByComparingTo(new BigDecimal("28.34"));
    }

    @Test
    void getStockProfile_재무지표_없으면_latestMetrics_null() {
        when(stockRepository.findByMarketAndTicker("US", "AAPL")).thenReturn(Optional.of(stock));
        when(financialMetricRepository.findTopByStockIdAndPeriodOrderByFiscalDateDesc(any(), any()))
                .thenReturn(Optional.empty());

        StockProfileResponse response = service.getStockProfile("AAPL");

        assertThat(response.ticker()).isEqualTo("AAPL");
        assertThat(response.latestMetrics()).isNull(); // 배치 수집 전
    }

    @Test
    void getStockProfile_섹터_없는_종목_섹터명_null() {
        Stock noSectorStock = Stock.builder()
                .ticker("XYZ").market("US").companyName("XYZ Corp.")
                .build();
        ReflectionTestUtils.setField(noSectorStock, "id", 2L);

        when(stockRepository.findByMarketAndTicker("US", "XYZ")).thenReturn(Optional.of(noSectorStock));
        when(financialMetricRepository.findTopByStockIdAndPeriodOrderByFiscalDateDesc(any(), any()))
                .thenReturn(Optional.empty());

        StockProfileResponse response = service.getStockProfile("XYZ");

        assertThat(response.sector()).isNull();
    }

    @Test
    void getStockProfile_존재하지_않는_종목_예외() {
        when(stockRepository.findByMarketAndTicker("US", "UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getStockProfile("UNKNOWN"))
                .isInstanceOf(BusinessException.class);
    }
}
