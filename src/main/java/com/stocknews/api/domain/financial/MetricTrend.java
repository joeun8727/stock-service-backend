package com.stocknews.api.domain.financial;

import com.stocknews.api.domain.stock.Stock;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "metric_trend",
    uniqueConstraints = @UniqueConstraint(columnNames = {"stock_id", "metric_name"})
)
@Getter
@NoArgsConstructor
public class MetricTrend {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(name = "metric_name", nullable = false, length = 50)
    private String metricName;

    @Column(nullable = false, precision = 12, scale = 6)
    private BigDecimal slope;

    @Column(name = "data_points", nullable = false)
    private Integer dataPoints;

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public MetricTrend(Stock stock, String metricName, BigDecimal slope, int dataPoints) {
        this.stock = stock;
        this.metricName = metricName;
        this.slope = slope;
        this.dataPoints = dataPoints;
        this.calculatedAt = LocalDateTime.now();
        this.createdAt = LocalDateTime.now();
    }

    public void update(BigDecimal slope, int dataPoints) {
        this.slope = slope;
        this.dataPoints = dataPoints;
        this.calculatedAt = LocalDateTime.now();
    }
}
