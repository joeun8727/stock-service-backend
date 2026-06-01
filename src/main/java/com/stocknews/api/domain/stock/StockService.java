package com.stocknews.api.domain.stock;

import com.stocknews.api.common.config.CacheKeys;
import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.domain.financial.FinancialMetric;
import com.stocknews.api.domain.financial.FinancialMetricRepository;
import com.stocknews.api.domain.stock.dto.StockProfileResponse;
import com.stocknews.api.domain.stock.dto.StockProfileResponse.LatestMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StockService {

    private final StockRepository stockRepository;
    private final FinancialMetricRepository financialMetricRepository;

    /**
     * GET /api/v1/stocks/{ticker}
     * 종목 기본정보 + 최신 분기 재무지표.
     * 배치 수집 전이면 latestMetrics = null.
     */
    @Cacheable(value = CacheKeys.STOCK_PROFILE, key = "#ticker.toUpperCase()")
    public StockProfileResponse getStockProfile(String ticker) {
        String upper = ticker.toUpperCase();
        Stock stock = stockRepository.findByMarketAndTicker("US", upper)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND, upper));

        Optional<FinancialMetric> metricOpt =
                financialMetricRepository.findTopByStockIdAndPeriodOrderByFiscalDateDesc(
                        stock.getId(), "quarterly");

        LatestMetrics latestMetrics = metricOpt.map(m -> new LatestMetrics(
                m.getRoe(), m.getRoa(), m.getRoic(),
                m.getPer(), m.getPbr(), m.getEps(),
                m.getDebtRatio(), m.getRevenueGrowthYoy(), m.getOperatingMargin()
        )).orElse(null);

        String sectorName = stock.getSector() != null ? stock.getSector().getName() : null;

        return new StockProfileResponse(
                stock.getTicker(),
                stock.getCompanyName(),
                sectorName,
                stock.getIndustry(),
                stock.getMarketCap(),
                stock.getExchange(),
                stock.getWebsite(),
                stock.getEmployeeCount(),
                stock.getIpoDate(),
                latestMetrics
        );
    }
}
