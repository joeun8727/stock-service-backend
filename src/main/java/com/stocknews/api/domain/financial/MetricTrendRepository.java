package com.stocknews.api.domain.financial;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MetricTrendRepository extends JpaRepository<MetricTrend, Long> {

    Optional<MetricTrend> findByStockIdAndMetricName(Long stockId, String metricName);

    List<MetricTrend> findAllByStockId(Long stockId);
}
