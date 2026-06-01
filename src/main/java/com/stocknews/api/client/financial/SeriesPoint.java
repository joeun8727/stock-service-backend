package com.stocknews.api.client.financial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record SeriesPoint(
        String period,
        @JsonProperty("v") BigDecimal value
) {}
