package com.stocknews.api.domain.news;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * 뉴스 수집 → LLM 분석 구간의 인메모리 snippet 전달 큐 (08-compliance.md).
 *
 * Finnhub snippet은 원문 요약이므로 DB 저장 금지.
 * 수집 시점에 큐에 적재 → LLM 분석 후 즉시 폐기.
 * 앱 재시작 시 큐는 소멸되며, 미처리 기사는 headline만으로 재처리됨.
 */
@Component
public class SnippetQueue {

    public record SnippetEntry(long articleId, String snippet) {}

    private final ConcurrentLinkedQueue<SnippetEntry> queue = new ConcurrentLinkedQueue<>();

    public void offer(long articleId, String snippet) {
        queue.offer(new SnippetEntry(articleId, snippet));
    }

    public SnippetEntry poll() {
        return queue.poll();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }
}
