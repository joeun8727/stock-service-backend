package com.stocknews.api.client.macro;

import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.domain.apicalllog.ApiCallLogRepository;
import com.stocknews.api.support.ClientTestSupport;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FredClientTest extends ClientTestSupport {

    FredClient client;

    @BeforeEach
    void setUp() {
        ApiCallLogRepository logRepository = mock(ApiCallLogRepository.class);
        client = new FredClient(testRestClient, "test-key", logRepository);
    }

    @Test
    void 거시지표_정상_응답_파싱() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"observations":[
                          {"date":"2025-01-01","value":"4.33"}
                        ]}
                        """));

        RawMacroData result = client.fetchSeries("DFF");

        assertThat(result.seriesId()).isEqualTo("DFF");
        assertThat(result.date()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("4.33"));
    }

    @Test
    void 결측값_dot_처리_시_null_value() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"observations":[
                          {"date":"2025-01-01","value":"."}
                        ]}
                        """));

        RawMacroData result = client.fetchSeries("GDP");

        assertThat(result.date()).isEqualTo(LocalDate.of(2025, 1, 1));
        assertThat(result.value()).isNull();
    }

    @Test
    void 빈_관측값_예외_발생() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"observations\":[]}"));

        assertThatThrownBy(() -> client.fetchSeries("DFF"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 요청_쿼리파라미터_검증() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("{\"observations\":[{\"date\":\"2025-01-01\",\"value\":\"5.0\"}]}"));

        client.fetchSeries("FEDFUNDS");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("/series/observations");
        assertThat(request.getPath()).contains("series_id=FEDFUNDS");
        assertThat(request.getPath()).contains("sort_order=desc");
        assertThat(request.getPath()).contains("limit=1");
    }
}
