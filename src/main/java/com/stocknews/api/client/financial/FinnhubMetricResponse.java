package com.stocknews.api.client.financial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubMetricResponse(
        FinnhubMetricData metric,
        FinnhubSeriesData series
) {}
