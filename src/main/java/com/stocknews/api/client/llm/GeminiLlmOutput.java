package com.stocknews.api.client.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GeminiLlmOutput(
        String summary,
        BigDecimal sentiment,   // -1.0 ~ 1.0
        Integer importance,     // 0 ~ 100 (null = 파싱 실패)
        String relevance        // HIGH | MEDIUM | LOW
) {}
