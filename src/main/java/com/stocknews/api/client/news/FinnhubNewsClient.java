package com.stocknews.api.client.news;

import com.stocknews.api.domain.apicalllog.ApiCallLog;
import com.stocknews.api.domain.apicalllog.ApiCallLogRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Slf4j
public class FinnhubNewsClient implements NewsProvider {

    private static final String PROVIDER = "finnhub";
    private static final String ENDPOINT = "/company-news";

    private final RestClient restClient;
    private final ApiCallLogRepository apiCallLogRepository;

    public FinnhubNewsClient(RestClient restClient, ApiCallLogRepository apiCallLogRepository) {
        this.restClient = restClient;
        this.apiCallLogRepository = apiCallLogRepository;
    }

    @Override
    @CircuitBreaker(name = "finnhub", fallbackMethod = "fallbackNews")
    @Retry(name = "finnhub")
    @RateLimiter(name = "finnhub")
    public List<RawNews> fetchCompanyNews(String ticker, LocalDate from, LocalDate to) {
        String status = "SUCCESS";
        try {
            List<FinnhubNewsItem> items = restClient.get()
                    .uri(u -> u.path(ENDPOINT)
                            .queryParam("symbol", ticker)
                            .queryParam("from", from.toString())
                            .queryParam("to", to.toString())
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<List<FinnhubNewsItem>>() {});

            if (items == null) return Collections.emptyList();

            return items.stream()
                    .map(item -> new RawNews(
                            item.headline(),
                            item.source(),
                            item.url(),
                            Instant.ofEpochSecond(item.datetime()).atZone(ZoneOffset.UTC).toLocalDateTime(),
                            item.summary()
                    ))
                    .toList();
        } catch (RestClientException e) {
            status = "ERROR";
            throw e;
        } finally {
            saveLog(ENDPOINT, status);
        }
    }

    public List<RawNews> fallbackNews(String ticker, LocalDate from, LocalDate to, Throwable t) {
        log.warn("Finnhub 뉴스 조회 fallback — ticker={}, 원인={}", ticker, t.getMessage());
        return Collections.emptyList();
    }

    private void saveLog(String endpoint, String status) {
        try {
            apiCallLogRepository.save(ApiCallLog.builder()
                    .provider(PROVIDER).endpoint(endpoint).status(status).build());
        } catch (Exception e) {
            log.error("ApiCallLog 저장 실패 (무시하고 계속): {}", e.getMessage());
        }
    }
}
