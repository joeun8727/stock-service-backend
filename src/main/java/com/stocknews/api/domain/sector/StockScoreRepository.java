package com.stocknews.api.domain.sector;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StockScoreRepository extends JpaRepository<StockScore, Long> {

    Optional<StockScore> findByStockIdAndScreenType(Long stockId, String screenType);

    @Query("""
            SELECT ss FROM StockScore ss
            JOIN FETCH ss.stock st
            JOIN FETCH st.sector
            WHERE ss.sector.id = :sectorId
              AND ss.screenType = :screenType
            ORDER BY ss.rankInSector ASC
            """)
    List<StockScore> findBySectorIdAndScreenTypeOrderByRank(
            @Param("sectorId") Long sectorId,
            @Param("screenType") String screenType);

    void deleteByStockIdAndScreenType(Long stockId, String screenType);
}
