package com.stocknews.api.client.financial;

import com.stocknews.api.domain.apicalllog.ApiCallLogRepository;
import com.stocknews.api.support.ClientTestSupport;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FinnhubFinancialClientTest extends ClientTestSupport {

    FinnhubFinancialClient client;

    @BeforeEach
    void setUp() {
        ApiCallLogRepository logRepository = mock(ApiCallLogRepository.class);
        client = new FinnhubFinancialClient(testRestClient, logRepository);
    }

    @Test
    void 프로필_정상_응답_파싱() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"name":"Apple Inc","ticker":"AAPL",
                         "exchange":"NASDAQ NMS",
                         "finnhubIndustry":"Technology",
                         "marketCapitalization":3059290.25,
                         "weburl":"https://www.apple.com/",
                         "logo":"https://static.finnhub.io/logo/apple.png",
                         "ipo":"1980-12-12"}
                        """));

        RawStockProfile profile = client.fetchProfile("AAPL");

        assertThat(profile).isNotNull();
        assertThat(profile.ticker()).isEqualTo("AAPL");
        assertThat(profile.companyName()).isEqualTo("Apple Inc");
        assertThat(profile.exchange()).isEqualTo("NASDAQ NMS");
        assertThat(profile.industry()).isEqualTo("Technology");
        assertThat(profile.marketCapMillions()).isEqualByComparingTo(new BigDecimal("3059290.25"));
        assertThat(profile.ipoDate()).isNotNull();
        assertThat(profile.ipoDate().getYear()).isEqualTo(1980);
    }

    @Test
    void 재무지표_정상_응답_파싱() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"metric":{
                          "roeTTM":1.4721,"roaTTM":0.3103,"roicTTM":0.5845,
                          "peExclExtraTTM":28.5,"pbAnnual":45.2,
                          "epsExclExtraItemsTTM":6.5,
                          "totalDebt/totalEquityAnnual":1.5,
                          "netInterestCoverageAnnual":null,
                          "revenueGrowthTTMYoy":0.062,
                          "operatingMarginTTM":0.314
                        },"series":{}}
                        """));

        List<RawFinancialMetric> result = client.fetchMetrics("AAPL");

        assertThat(result).hasSize(1);
        RawFinancialMetric m = result.get(0);
        assertThat(m.period()).isEqualTo("annual");
        assertThat(m.roe()).isEqualByComparingTo(new BigDecimal("1.4721"));
        assertThat(m.roa()).isEqualByComparingTo(new BigDecimal("0.3103"));
        assertThat(m.per()).isEqualByComparingTo(new BigDecimal("28.5"));
        assertThat(m.revenueGrowthYoy()).isEqualByComparingTo(new BigDecimal("0.062"));
        assertThat(m.interestCoverage()).isNull();  // netInterestCoverageAnnual: null
        assertThat(m.ocfToNi()).isNull();
    }

    @Test
    void 재무지표_null_metric_빈_리스트_반환() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"metric\":null,\"series\":{}}"));

        List<RawFinancialMetric> result = client.fetchMetrics("UNKNOWN");

        assertThat(result).isEmpty();
    }

    @Test
    void 재무지표_요청_경로_검증() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"metric\":null,\"series\":{}}"));

        client.fetchMetrics("NVDA");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("/stock/metric");
        assertThat(request.getPath()).contains("symbol=NVDA");
        assertThat(request.getPath()).contains("metric=all");
    }
}
