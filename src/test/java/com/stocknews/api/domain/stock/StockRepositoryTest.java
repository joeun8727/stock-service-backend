package com.stocknews.api.domain.stock;

import com.stocknews.api.domain.sector.Sector;
import com.stocknews.api.domain.sector.SectorRepository;
import com.stocknews.api.support.RepositoryTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StockRepositoryTest extends RepositoryTestSupport {

    @Autowired
    StockRepository stockRepository;

    @Autowired
    SectorRepository sectorRepository;

    @Autowired
    EntityManager em;

    private Sector sector;

    @BeforeEach
    void setUp() {
        sector = sectorRepository.saveAndFlush(
                Sector.builder().code("SEMICONDUCTOR").name("반도체").build()
        );
    }

    @Test
    void 저장_후_기본정보_조회() {
        Stock stock = Stock.builder()
                .ticker("NVDA").market("US").companyName("NVIDIA Corporation")
                .sector(sector).industry("Semiconductors")
                .marketCap(new BigDecimal("3000000000000.00"))
                .exchange("NASDAQ")
                .build();

        Stock saved = stockRepository.saveAndFlush(stock);
        em.clear();

        Stock found = stockRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getTicker()).isEqualTo("NVDA");
        assertThat(found.getMarket()).isEqualTo("US");
        assertThat(found.getCompanyName()).isEqualTo("NVIDIA Corporation");
        assertThat(found.getIsGrowthCandidate()).isFalse();
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getUpdatedAt()).isNull();
    }

    @Test
    void market_ticker_유니크_제약_중복_저장시_예외() {
        stockRepository.saveAndFlush(
                Stock.builder().ticker("AAPL").market("US").companyName("Apple Inc.").build()
        );

        assertThatThrownBy(() ->
                stockRepository.saveAndFlush(
                        Stock.builder().ticker("AAPL").market("US").companyName("Apple Duplicate").build()
                )
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByMarketAndTicker_존재하는_종목() {
        stockRepository.saveAndFlush(
                Stock.builder().ticker("MSFT").market("US").companyName("Microsoft").build()
        );

        Optional<Stock> result = stockRepository.findByMarketAndTicker("US", "MSFT");

        assertThat(result).isPresent();
        assertThat(result.get().getCompanyName()).isEqualTo("Microsoft");
    }

    @Test
    void findByMarketAndTicker_없는_종목() {
        Optional<Stock> result = stockRepository.findByMarketAndTicker("US", "UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void updateGrowthCandidate_플래그와_updatedAt_갱신() {
        Stock stock = stockRepository.saveAndFlush(
                Stock.builder().ticker("TSLA").market("US").companyName("Tesla").build()
        );
        assertThat(stock.getIsGrowthCandidate()).isFalse();

        stock.updateGrowthCandidate(true);
        stockRepository.flush();
        em.clear();

        Stock updated = stockRepository.findById(stock.getId()).orElseThrow();
        assertThat(updated.getIsGrowthCandidate()).isTrue();
        assertThat(updated.getUpdatedAt()).isNotNull();
    }
}
