package com.stocknews.api.client.financial;

import com.stocknews.api.domain.apicalllog.ApiCallLog;
import com.stocknews.api.domain.apicalllog.ApiCallLogRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

@Slf4j
public class FinnhubFinancialClient implements FinancialProvider {

    private static final String PROVIDER = "finnhub";
    private static final String METRIC_ENDPOINT  = "/stock/metric";
    private static final String PROFILE_ENDPOINT = "/stock/profile2";
    private static final String SYMBOL_ENDPOINT  = "/stock/symbol";

    private final RestClient restClient;
    private final ApiCallLogRepository apiCallLogRepository;

    public FinnhubFinancialClient(RestClient restClient, ApiCallLogRepository apiCallLogRepository) {
        this.restClient = restClient;
        this.apiCallLogRepository = apiCallLogRepository;
    }

    @Override
    @CircuitBreaker(name = "finnhub", fallbackMethod = "fallbackMetrics")
    @Retry(name = "finnhub")
    @RateLimiter(name = "finnhub")
    public List<RawFinancialMetric> fetchMetrics(String ticker) {
        String status = "SUCCESS";
        try {
            FinnhubMetricResponse response = restClient.get()
                    .uri(u -> u.path(METRIC_ENDPOINT)
                            .queryParam("symbol", ticker)
                            .queryParam("metric", "all")
                            .build())
                    .retrieve()
                    .body(FinnhubMetricResponse.class);

            if (response == null || response.metric() == null) return Collections.emptyList();

            FinnhubMetricData m = response.metric();
            FinnhubSeriesData series = response.series();

            // series.quarterly 에서 최신값(index 0) 및 리스트 추출
            List<SeriesPoint> grossMarginSeries    = seriesList(series, "grossMargin");
            List<SeriesPoint> roicSeries           = seriesList(series, "roic");
            List<SeriesPoint> opMarginSeries       = seriesList(series, "operatingMargin");
            List<SeriesPoint> fcfMarginSeries      = seriesList(series, "fcfMargin");

            BigDecimal fcfMarginLatest = fcfMarginSeries.isEmpty() ? null
                    : fcfMarginSeries.get(0).value();
            BigDecimal roicLatest = roicSeries.isEmpty() ? null
                    : roicSeries.get(0).value();
            // roicTTM 무료 티어 미제공 시 series 최신 분기값으로 폴백
            BigDecimal roicSnapshot = m.roicTTM() != null ? toBd(m.roicTTM()) : roicLatest;

            return List.of(new RawFinancialMetric(
                    "quarterly",
                    LocalDate.now(),
                    toBd(m.roeTTM()),
                    toBd(m.roaTTM()),
                    roicSnapshot,
                    toBd(m.peExclExtraTTM()),
                    toBd(m.pbAnnual()),
                    toBd(m.epsExclExtraItemsTTM()),
                    toBd(m.debtToEquityQuarterly()),
                    toBd(m.netInterestCoverageTTM()),
                    toBd(m.revenueGrowthTTMYoy()),
                    toBd(m.operatingMarginTTM()),
                    null,                           // ocfToNi — 무료 티어 미제공
                    toBd(m.psTTM()),
                    toBd(m.pegTTM()),
                    toBd(m.grossMarginTTM()),
                    fcfMarginLatest,
                    toBd(m.marketCapitalization()),
                    grossMarginSeries,
                    roicSeries,
                    opMarginSeries,
                    fcfMarginSeries
            ));
        } catch (RestClientException e) {
            status = "ERROR";
            throw e;
        } finally {
            saveLog(METRIC_ENDPOINT, status);
        }
    }

    @Override
    @CircuitBreaker(name = "finnhub", fallbackMethod = "fallbackProfile")
    @Retry(name = "finnhub")
    @RateLimiter(name = "finnhub")
    public RawStockProfile fetchProfile(String ticker) {
        String status = "SUCCESS";
        try {
            FinnhubProfileResponse p = restClient.get()
                    .uri(u -> u.path(PROFILE_ENDPOINT)
                            .queryParam("symbol", ticker)
                            .build())
                    .retrieve()
                    .body(FinnhubProfileResponse.class);

            if (p == null || p.ticker() == null) return null;

            LocalDate ipoDate = null;
            if (p.ipo() != null && !p.ipo().isBlank()) {
                try { ipoDate = LocalDate.parse(p.ipo()); }
                catch (Exception e) { log.warn("IPO 날짜 파싱 실패 — ticker={}, ipo={}", ticker, p.ipo()); }
            }

            return new RawStockProfile(
                    p.ticker(), p.name(), p.exchange(), p.finnhubIndustry(),
                    p.marketCapitalization(), p.weburl(), p.logo(), ipoDate
            );
        } catch (RestClientException e) {
            status = "ERROR";
            throw e;
        } finally {
            saveLog(PROFILE_ENDPOINT, status);
        }
    }

    @Override
    @CircuitBreaker(name = "finnhub", fallbackMethod = "fallbackSymbols")
    @Retry(name = "finnhub")
    @RateLimiter(name = "finnhub")
    public List<FinnhubSymbolItem> fetchSymbols(String exchange) {
        String status = "SUCCESS";
        try {
            List<FinnhubSymbolItem> result = restClient.get()
                    .uri(u -> u.path(SYMBOL_ENDPOINT)
                            .queryParam("exchange", exchange)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return result != null ? result : Collections.emptyList();
        } catch (RestClientException e) {
            status = "ERROR";
            throw e;
        } finally {
            saveLog(SYMBOL_ENDPOINT, status);
        }
    }

    // ── fallback ──────────────────────────────────────────────────────────────

    public List<RawFinancialMetric> fallbackMetrics(String ticker, Throwable t) {
        log.warn("Finnhub 재무지표 조회 fallback — ticker={}, 원인={}", ticker, t.getMessage());
        return Collections.emptyList();
    }

    public RawStockProfile fallbackProfile(String ticker, Throwable t) {
        log.warn("Finnhub 프로필 조회 fallback — ticker={}, 원인={}", ticker, t.getMessage());
        return null;
    }

    public List<FinnhubSymbolItem> fallbackSymbols(String exchange, Throwable t) {
        log.warn("Finnhub 종목 목록 조회 fallback — exchange={}, 원인={}", exchange, t.getMessage());
        return Collections.emptyList();
    }

    // ── 헬퍼 ─────────────────────────────────────────────────────────────────

    private List<SeriesPoint> seriesList(FinnhubSeriesData series, String name) {
        if (series == null || series.quarterly() == null) return Collections.emptyList();
        List<SeriesPoint> list = switch (name) {
            case "grossMargin"     -> series.quarterly().grossMargin();
            case "roic"            -> series.quarterly().roic();
            case "operatingMargin" -> series.quarterly().operatingMargin();
            case "fcfMargin"       -> series.quarterly().fcfMargin();
            default -> null;
        };
        return list != null ? list : Collections.emptyList();
    }

    private void saveLog(String endpoint, String status) {
        try {
            apiCallLogRepository.save(ApiCallLog.builder()
                    .provider(PROVIDER).endpoint(endpoint).status(status).build());
        } catch (Exception e) {
            log.error("ApiCallLog 저장 실패 (무시하고 계속): {}", e.getMessage());
        }
    }

    private static BigDecimal toBd(Double value) {
        return value != null ? BigDecimal.valueOf(value) : null;
    }
}
