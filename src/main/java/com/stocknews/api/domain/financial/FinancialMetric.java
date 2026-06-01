package com.stocknews.api.domain.financial;

import com.stocknews.api.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "financial_metric",
    uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "period", "fiscal_date"})
)
@Getter
@NoArgsConstructor
public class FinancialMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(nullable = false, length = 10)
    private String period;  // annual / quarterly

    @Column(name = "fiscal_date", nullable = false)
    private LocalDate fiscalDate;

    @Column(precision = 10, scale = 4)
    private BigDecimal roe;

    @Column(precision = 10, scale = 4)
    private BigDecimal roa;

    @Column(precision = 10, scale = 4)
    private BigDecimal roic;

    @Column(precision = 10, scale = 4)
    private BigDecimal per;

    @Column(precision = 10, scale = 4)
    private BigDecimal pbr;

    @Column(precision = 10, scale = 4)
    private BigDecimal eps;

    @Column(name = "debt_ratio", precision = 10, scale = 4)
    private BigDecimal debtRatio;

    @Column(name = "interest_coverage", precision = 10, scale = 4)
    private BigDecimal interestCoverage;

    @Column(name = "revenue_growth_yoy", precision = 10, scale = 4)
    private BigDecimal revenueGrowthYoy;

    @Column(name = "operating_margin", precision = 10, scale = 4)
    private BigDecimal operatingMargin;

    @Column(name = "ocf_to_ni", precision = 10, scale = 4)
    private BigDecimal ocfToNi;

    // 신규 (V4 마이그레이션)
    @Column(name = "psr", precision = 10, scale = 4)
    private BigDecimal psr;

    @Column(name = "peg", precision = 10, scale = 4)
    private BigDecimal peg;

    @Column(name = "gross_margin", precision = 10, scale = 4)
    private BigDecimal grossMargin;

    @Column(name = "fcf_margin", precision = 10, scale = 4)
    private BigDecimal fcfMargin;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public FinancialMetric(Stock stock, String period, LocalDate fiscalDate,
                           BigDecimal roe, BigDecimal roa, BigDecimal roic,
                           BigDecimal per, BigDecimal pbr, BigDecimal eps,
                           BigDecimal debtRatio, BigDecimal interestCoverage,
                           BigDecimal revenueGrowthYoy, BigDecimal operatingMargin,
                           BigDecimal ocfToNi,
                           BigDecimal psr, BigDecimal peg,
                           BigDecimal grossMargin, BigDecimal fcfMargin) {
        this.stock = stock;
        this.period = period;
        this.fiscalDate = fiscalDate;
        this.roe = roe;
        this.roa = roa;
        this.roic = roic;
        this.per = per;
        this.pbr = pbr;
        this.eps = eps;
        this.debtRatio = debtRatio;
        this.interestCoverage = interestCoverage;
        this.revenueGrowthYoy = revenueGrowthYoy;
        this.operatingMargin = operatingMargin;
        this.ocfToNi = ocfToNi;
        this.psr = psr;
        this.peg = peg;
        this.grossMargin = grossMargin;
        this.fcfMargin = fcfMargin;
        this.createdAt = LocalDateTime.now();
    }
}
