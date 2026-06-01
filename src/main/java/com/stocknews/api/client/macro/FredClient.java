package com.stocknews.api.client.macro;

import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.domain.apicalllog.ApiCallLog;
import com.stocknews.api.domain.apicalllog.ApiCallLogRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Slf4j
public class FredClient implements MacroProvider {

    private static final String PROVIDER = "fred";
    private static final String OBSERVATIONS_ENDPOINT = "/series/observations";

    private final RestClient restClient;
    private final String apiKey;
    private final ApiCallLogRepository apiCallLogRepository;

    public FredClient(RestClient restClient, String apiKey, ApiCallLogRepository apiCallLogRepository) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.apiCallLogRepository = apiCallLogRepository;
    }

    @Override
    @CircuitBreaker(name = "fred", fallbackMethod = "fallbackSeries")
    @Retry(name = "fred")
    public RawMacroData fetchSeries(String seriesId) {
        String status = "SUCCESS";
        try {
            FredObservationsResponse response = restClient.get()
                    .uri(u -> u.path(OBSERVATIONS_ENDPOINT)
                            .queryParam("series_id", seriesId)
                            .queryParam("api_key", apiKey)
                            .queryParam("file_type", "json")
                            .queryParam("sort_order", "desc")
                            .queryParam("limit", "1")
                            .build())
                    .retrieve()
                    .body(FredObservationsResponse.class);

            List<FredObservationsResponse.FredObservation> obs =
                    (response != null) ? response.observations() : null;

            if (obs == null || obs.isEmpty()) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR,
                        "FRED 관측값 없음 — seriesId=" + seriesId);
            }

            FredObservationsResponse.FredObservation latest = obs.get(0);
            LocalDate date = LocalDate.parse(latest.date());
            BigDecimal value = ".".equals(latest.value()) ? null : new BigDecimal(latest.value());

            return new RawMacroData(seriesId, date, value, "");
        } catch (RestClientException e) {
            status = "ERROR";
            throw e;
        } finally {
            saveLog(OBSERVATIONS_ENDPOINT, status);
        }
    }

    public RawMacroData fallbackSeries(String seriesId, Throwable t) {
        log.warn("FRED 거시지표 조회 fallback — seriesId={}, 원인={}", seriesId, t.getMessage());
        throw new BusinessException(ErrorCode.EXTERNAL_API_CIRCUIT_OPEN,
                "FRED 서비스 일시 중단 — seriesId=" + seriesId);
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
