package com.stocknews.api.client.financial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubMetricData(
        // 기존
        Double roeTTM,
        Double roaTTM,
        Double roicTTM,
        Double peExclExtraTTM,
        Double pbAnnual,
        Double epsExclExtraItemsTTM,
        @JsonProperty("totalDebt/totalEquityQuarterly") Double debtToEquityQuarterly,
        Double netInterestCoverageTTM,
        Double revenueGrowthTTMYoy,
        Double operatingMarginTTM,
        // 신규
        Double psTTM,
        Double pegTTM,
        Double grossMarginTTM,
        Double marketCapitalization   // 단위: 백만 USD
) {}
