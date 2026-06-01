package com.stocknews.api.client.llm;

import java.math.BigDecimal;

public record LLMAnalysis(
        String summary,          // 3줄 이내 한국어 요약
        BigDecimal sentiment,    // -1.0 ~ 1.0
        Integer importance,      // 0 ~ 100 (null = 파싱 실패)
        String relevance         // HIGH | MEDIUM | LOW
) {}
