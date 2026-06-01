package com.stocknews.api.client.financial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubSeriesData(FinnhubQuarterlySeries quarterly) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FinnhubQuarterlySeries(
            List<SeriesPoint> grossMargin,
            List<SeriesPoint> roic,
            List<SeriesPoint> operatingMargin,
            List<SeriesPoint> fcfMargin
    ) {}
}
