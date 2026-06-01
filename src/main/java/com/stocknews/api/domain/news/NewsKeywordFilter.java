package com.stocknews.api.domain.news;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * LLM 비용 절감을 위한 1차 키워드 필터 (06-news-llm.md).
 * 통과 기준: 헤드라인 또는 스니펫에 설정된 키워드 중 하나 이상 포함.
 */
@Slf4j
@Component
public class NewsKeywordFilter {

    private final Set<String> includeKeywords;

    public NewsKeywordFilter(@Value("${news.keywords.include}") String keywordsRaw) {
        this.includeKeywords = Arrays.stream(keywordsRaw.split(","))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(k -> !k.isBlank())
                .collect(Collectors.toUnmodifiableSet());
        log.info("뉴스 키워드 필터 초기화: {}개 키워드 로드", includeKeywords.size());
    }

    public boolean passes(String headline, String snippet) {
        String combined = ((headline != null ? headline : "") + " "
                + (snippet != null ? snippet : "")).toLowerCase();
        return includeKeywords.stream().anyMatch(combined::contains);
    }
}
