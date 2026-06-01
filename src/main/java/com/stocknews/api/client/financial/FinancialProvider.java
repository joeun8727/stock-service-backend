package com.stocknews.api.client.financial;

import java.util.List;

public interface FinancialProvider {

    List<RawFinancialMetric> fetchMetrics(String ticker);

    RawStockProfile fetchProfile(String ticker);

    List<FinnhubSymbolItem> fetchSymbols(String exchange);
}
