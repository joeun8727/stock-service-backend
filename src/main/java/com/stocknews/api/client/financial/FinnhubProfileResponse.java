package com.stocknews.api.client.financial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubProfileResponse(
        String name,
        String ticker,
        String exchange,
        @JsonProperty("finnhubIndustry") String finnhubIndustry,
        @JsonProperty("marketCapitalization") BigDecimal marketCapitalization, // 단위: 백만 USD
        String weburl,
        String logo,
        String ipo
) {}
