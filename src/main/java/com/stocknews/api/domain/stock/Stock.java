package com.stocknews.api.domain.stock;

import com.stocknews.api.domain.sector.Sector;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "stock",
    uniqueConstraints = @UniqueConstraint(columnNames = {"market", "ticker"})
)
@Getter
@NoArgsConstructor
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String ticker;

    @Column(nullable = false, length = 10)
    private String market;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id")
    private Sector sector;

    @Column(length = 100)
    private String industry;

    @Column(name = "market_cap", precision = 20, scale = 2)
    private BigDecimal marketCap;

    @Column(length = 50)
    private String exchange;

    private String website;

    @Column(name = "employee_count")
    private Integer employeeCount;

    @Column(name = "ipo_date")
    private LocalDate ipoDate;

    @Column(name = "is_growth_candidate")
    private Boolean isGrowthCandidate = false;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Stock(String ticker, String market, String companyName, Sector sector,
                 String industry, BigDecimal marketCap, String exchange,
                 String website, Integer employeeCount, LocalDate ipoDate) {
        this.ticker = ticker;
        this.market = market;
        this.companyName = companyName;
        this.sector = sector;
        this.industry = industry;
        this.marketCap = marketCap;
        this.exchange = exchange;
        this.website = website;
        this.employeeCount = employeeCount;
        this.ipoDate = ipoDate;
        this.createdAt = LocalDateTime.now();
    }

    public void updateGrowthCandidate(boolean isGrowthCandidate) {
        this.isGrowthCandidate = isGrowthCandidate;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateProfile(String companyName, String industry, BigDecimal marketCapMillions,
                              String exchange, String website, LocalDate ipoDate) {
        if (companyName != null) this.companyName = companyName;
        if (industry != null) this.industry = industry;
        if (marketCapMillions != null) {
            // Finnhub은 marketCap을 백만 USD 단위로 반환 → 실제 USD로 변환
            this.marketCap = marketCapMillions.multiply(BigDecimal.valueOf(1_000_000));
        }
        if (exchange != null) this.exchange = exchange;
        if (website != null) this.website = website;
        if (ipoDate != null) this.ipoDate = ipoDate;
        this.updatedAt = LocalDateTime.now();
    }
}
