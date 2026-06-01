package com.stocknews.api.client.financial;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubSymbolItem(
        String symbol,
        String description,
        String type,        // "Common Stock", "ETP", etc.
        String currency,
        String mic          // exchange MIC code
) {}
