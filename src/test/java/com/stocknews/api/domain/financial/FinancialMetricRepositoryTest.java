package com.stocknews.api.domain.financial;

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
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FinancialMetricRepositoryTest extends RepositoryTestSupport {

    @Autowired
    FinancialMetricRepository financialMetricRepository;

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
                Sector.builder().code("SEMICONDUCTOR").name("반도체").build()
        );
        stock = stockRepository.saveAndFlush(
                Stock.builder().ticker("NVDA").market("US").companyName("NVIDIA").sector(sector).build()
        );
    }

    @Test
    void 저장_후_DECIMAL_정밀도_확인() {
        FinancialMetric metric = FinancialMetric.builder()
                .stock(stock)
                .period("annual")
                .fiscalDate(LocalDate.of(2024, 12, 31))
                .roe(new BigDecimal("0.1234"))
                .roa(new BigDecimal("0.0567"))
                .roic(new BigDecimal("0.1500"))
                .per(new BigDecimal("32.5000"))
                .pbr(new BigDecimal("4.1000"))
                .eps(new BigDecimal("25.0000"))
                .debtRatio(new BigDecimal("0.3200"))
                .revenueGrowthYoy(new BigDecimal("0.2200"))
                .operatingMargin(new BigDecimal("0.5500"))
                .build();

        FinancialMetric saved = financialMetricRepository.saveAndFlush(metric);
        em.clear();

        FinancialMetric found = financialMetricRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getRoe()).isEqualByComparingTo(new BigDecimal("0.1234"));
        assertThat(found.getPer()).isEqualByComparingTo(new BigDecimal("32.5000"));
        assertThat(found.getRevenueGrowthYoy()).isEqualByComparingTo(new BigDecimal("0.2200"));
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    void stock_period_fiscalDate_유니크_제약_중복_저장시_예외() {
        LocalDate fiscalDate = LocalDate.of(2024, 12, 31);
        financialMetricRepository.saveAndFlush(
                FinancialMetric.builder().stock(stock).period("annual").fiscalDate(fiscalDate).build()
        );

        assertThatThrownBy(() ->
                financialMetricRepository.saveAndFlush(
                        FinancialMetric.builder().stock(stock).period("annual").fiscalDate(fiscalDate).build()
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByStockIdAndPeriodOrderByFiscalDateDesc_최신순_정렬() {
        financialMetricRepository.saveAndFlush(
                FinancialMetric.builder().stock(stock).period("annual").fiscalDate(LocalDate.of(2022, 12, 31)).build()
        );
        financialMetricRepository.saveAndFlush(
                FinancialMetric.builder().stock(stock).period("annual").fiscalDate(LocalDate.of(2024, 12, 31)).build()
        );
        financialMetricRepository.saveAndFlush(
                FinancialMetric.builder().stock(stock).period("annual").fiscalDate(LocalDate.of(2023, 12, 31)).build()
        );
        em.clear();

        List<FinancialMetric> results = financialMetricRepository
                .findByStockIdAndPeriodOrderByFiscalDateDesc(stock.getId(), "annual");

        assertThat(results).hasSize(3);
        assertThat(results.get(0).getFiscalDate()).isEqualTo(LocalDate.of(2024, 12, 31));
        assertThat(results.get(2).getFiscalDate()).isEqualTo(LocalDate.of(2022, 12, 31));
    }

    @Test
    void period_quarterly와_annual_구분_조회() {
        financialMetricRepository.saveAndFlush(
                FinancialMetric.builder().stock(stock).period("annual").fiscalDate(LocalDate.of(2024, 12, 31)).build()
        );
        financialMetricRepository.saveAndFlush(
                FinancialMetric.builder().stock(stock).period("quarterly").fiscalDate(LocalDate.of(2024, 9, 30)).build()
        );
        em.clear();

        List<FinancialMetric> annuals = financialMetricRepository
                .findByStockIdAndPeriodOrderByFiscalDateDesc(stock.getId(), "annual");
        List<FinancialMetric> quarterlies = financialMetricRepository
                .findByStockIdAndPeriodOrderByFiscalDateDesc(stock.getId(), "quarterly");

        assertThat(annuals).hasSize(1);
        assertThat(quarterlies).hasSize(1);
    }
}
