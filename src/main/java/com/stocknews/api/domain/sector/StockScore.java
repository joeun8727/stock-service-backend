package com.stocknews.api.domain.sector;

import com.stocknews.api.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "stock_score",
    uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "screen_type"})
)
@Getter
@NoArgsConstructor
public class StockScore {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", nullable = false)
    private Sector sector;

    /** LARGE_CAP / GROWTH */
    @Column(name = "screen_type", nullable = false, length = 20)
    private String screenType;

    /** GROWTH_TECH / TRADITIONAL */
    @Column(name = "sector_group", nullable = false, length = 20)
    private String sectorGroup;

    @Column(name = "total_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal totalScore;

    @Column(name = "rank_in_sector", nullable = false)
    private Integer rankInSector;

    @Column(name = "score_detail", columnDefinition = "JSON")
    private String scoreDetail;

    @Column(name = "scored_at", nullable = false)
    private LocalDateTime scoredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public StockScore(Stock stock, Sector sector, String screenType, String sectorGroup,
                      BigDecimal totalScore, int rankInSector, String scoreDetail) {
        this.stock = stock;
        this.sector = sector;
        this.screenType = screenType;
        this.sectorGroup = sectorGroup;
        this.totalScore = totalScore;
        this.rankInSector = rankInSector;
        this.scoreDetail = scoreDetail;
        this.scoredAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    public void update(BigDecimal totalScore, int rankInSector, String scoreDetail, String sectorGroup) {
        this.totalScore = totalScore;
        this.rankInSector = rankInSector;
        this.scoreDetail = scoreDetail;
        this.sectorGroup = sectorGroup;
        this.scoredAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
