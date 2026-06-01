package com.stocknews.api.domain.financial;

import com.stocknews.api.common.config.CacheKeys;
import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.domain.financial.dto.FinancialMetricResponse;
import com.stocknews.api.domain.financial.dto.FinancialMetricResponse.PeriodMetric;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinancialService {

    private final StockRepository stockRepository;
    private final FinancialMetricRepository financialMetricRepository;

    /**
     * GET /api/v1/stocks/{ticker}/financials?period=annual|quarterly&limit=4
     * 종목 재무지표 시계열 (최신순, limit개).
     */
    @Cacheable(value = CacheKeys.STOCK_METRICS, key = "#ticker.toUpperCase() + '_' + #period + '_' + #limit")
    public FinancialMetricResponse getFinancials(String ticker, String period, int limit) {
        String upper = ticker.toUpperCase();
        Stock stock = stockRepository.findByMarketAndTicker("US", upper)
                .orElseThrow(() -> new BusinessException(ErrorCode.STOCK_NOT_FOUND, upper));

        List<FinancialMetric> metrics =
                financialMetricRepository.findByStockIdAndPeriodOrderByFiscalDateDesc(stock.getId(), period)
                        .stream()
                        .limit(limit)
                        .toList();

        List<PeriodMetric> periodMetrics = metrics.stream()
                .map(m -> new PeriodMetric(
                        m.getFiscalDate(),
                        m.getRoe(), m.getRoa(), m.getRoic(),
                        m.getPer(), m.getPbr(), m.getEps(),
                        m.getDebtRatio(), m.getInterestCoverage(),
                        m.getRevenueGrowthYoy(), m.getOperatingMargin(), m.getOcfToNi()
                ))
                .toList();

        return new FinancialMetricResponse(upper, period, periodMetrics);
    }
}
