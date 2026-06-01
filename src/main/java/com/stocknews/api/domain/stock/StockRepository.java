package com.stocknews.api.domain.stock;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface StockRepository extends JpaRepository<Stock, Long> {

    Optional<Stock> findByMarketAndTicker(String market, String ticker);

    // 스케줄러: 뉴스 수집 대상 전체 종목
    List<Stock> findAllByMarket(String market);

    // 대형주 스크리닝: 섹터 내 시가총액 내림차순
    List<Stock> findBySectorIdAndMarketOrderByMarketCapDesc(Long sectorId, String market);

    // 성장주 스크리닝: 시가총액 상한($500B) 이하, 시가총액 있는 종목만
    @Query("SELECT s FROM Stock s WHERE s.sector.id = :sectorId AND s.market = :market AND s.marketCap IS NOT NULL AND s.marketCap <= :maxMarketCap")
    List<Stock> findGrowthCandidatesBySector(@Param("sectorId") Long sectorId,
                                              @Param("market") String market,
                                              @Param("maxMarketCap") BigDecimal maxMarketCap);
}
