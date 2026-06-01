package com.stocknews.api.domain.financial;

import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.domain.financial.dto.FinancialMetricResponse;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FinancialServiceTest {

    @Mock StockRepository stockRepository;
    @Mock FinancialMetricRepository financialMetricRepository;

    FinancialService service;
    Stock stock;

    @BeforeEach
    void setUp() {
        service = new FinancialService(stockRepository, financialMetricRepository);
        stock = Stock.builder().ticker("AAPL").market("US").companyName("Apple Inc.").build();
        ReflectionTestUtils.setField(stock, "id", 1L);
    }

    @Test
    void getFinancials_분기_4개_정상_반환() {
        List<FinancialMetric> metrics = List.of(
                metric(LocalDate.now()),
                metric(LocalDate.now().minusMonths(3)),
                metric(LocalDate.now().minusMonths(6)),
                metric(LocalDate.now().minusMonths(9))
        );

        when(stockRepository.findByMarketAndTicker("US", "AAPL")).thenReturn(Optional.of(stock));
        when(financialMetricRepository.findByStockIdAndPeriodOrderByFiscalDateDesc(eq(1L), eq("quarterly")))
                .thenReturn(metrics);

        FinancialMetricResponse response = service.getFinancials("aapl", "quarterly", 4);

        assertThat(response.ticker()).isEqualTo("AAPL");
        assertThat(response.period()).isEqualTo("quarterly");
        assertThat(response.metrics()).hasSize(4);
        assertThat(response.metrics().get(0).revenueGrowthYoy()).isEqualByComparingTo(new BigDecimal("0.20"));
    }

    @Test
    void getFinancials_limit_적용_초과분_제외() {
        List<FinancialMetric> metrics = List.of(
                metric(LocalDate.now()),
                metric(LocalDate.now().minusMonths(3)),
                metric(LocalDate.now().minusMonths(6))
        );

        when(stockRepository.findByMarketAndTicker("US", "AAPL")).thenReturn(Optional.of(stock));
        when(financialMetricRepository.findByStockIdAndPeriodOrderByFiscalDateDesc(eq(1L), eq("quarterly")))
                .thenReturn(metrics);

        FinancialMetricResponse response = service.getFinancials("AAPL", "quarterly", 2);

        assertThat(response.metrics()).hasSize(2);
    }

    @Test
    void getFinancials_데이터_없으면_빈_목록() {
        when(stockRepository.findByMarketAndTicker("US", "AAPL")).thenReturn(Optional.of(stock));
        when(financialMetricRepository.findByStockIdAndPeriodOrderByFiscalDateDesc(eq(1L), eq("annual")))
                .thenReturn(List.of());

        FinancialMetricResponse response = service.getFinancials("AAPL", "annual", 4);

        assertThat(response.metrics()).isEmpty();
    }

    @Test
    void getFinancials_존재하지_않는_종목_예외() {
        when(stockRepository.findByMarketAndTicker("US", "UNKNOWN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getFinancials("UNKNOWN", "quarterly", 4))
                .isInstanceOf(BusinessException.class);
    }

    private FinancialMetric metric(LocalDate fiscalDate) {
        return FinancialMetric.builder()
                .stock(stock).period("quarterly").fiscalDate(fiscalDate)
                .roe(new BigDecimal("1.56")).roa(new BigDecimal("0.29"))
                .roic(new BigDecimal("0.45")).per(new BigDecimal("28.0"))
                .pbr(new BigDecimal("45.0")).eps(new BigDecimal("6.43"))
                .debtRatio(new BigDecimal("1.72")).interestCoverage(new BigDecimal("20.0"))
                .revenueGrowthYoy(new BigDecimal("0.20")).operatingMargin(new BigDecimal("0.31"))
                .build();
    }
}
