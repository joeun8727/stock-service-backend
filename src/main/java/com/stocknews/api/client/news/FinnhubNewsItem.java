package com.stocknews.api.client.news;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubNewsItem(
        String headline,
        String source,
        String summary,
        String url,
        long datetime    // Unix epoch seconds
) {}
