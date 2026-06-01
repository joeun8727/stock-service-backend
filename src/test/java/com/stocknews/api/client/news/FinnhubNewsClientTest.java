package com.stocknews.api.client.news;

import com.stocknews.api.domain.apicalllog.ApiCallLogRepository;
import com.stocknews.api.support.ClientTestSupport;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class FinnhubNewsClientTest extends ClientTestSupport {

    FinnhubNewsClient client;

    @BeforeEach
    void setUp() {
        ApiCallLogRepository logRepository = mock(ApiCallLogRepository.class);
        client = new FinnhubNewsClient(testRestClient, logRepository);
    }

    @Test
    void 뉴스_정상_응답_파싱() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        [{"category":"company","datetime":1735689600,
                         "headline":"AAPL Q1 Earnings Beat",
                         "id":1001,"image":"","related":"AAPL",
                         "source":"Reuters",
                         "summary":"Apple beat Q1 expectations.",
                         "url":"https://reuters.com/aapl-q1"}]
                        """));

        List<RawNews> result = client.fetchCompanyNews("AAPL",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 31));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).headline()).isEqualTo("AAPL Q1 Earnings Beat");
        assertThat(result.get(0).source()).isEqualTo("Reuters");
        assertThat(result.get(0).sourceUrl()).isEqualTo("https://reuters.com/aapl-q1");
        assertThat(result.get(0).snippet()).isEqualTo("Apple beat Q1 expectations.");
        assertThat(result.get(0).publishedAt()).isNotNull();
    }

    @Test
    void 빈_배열_응답_시_빈_리스트_반환() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"));

        List<RawNews> result = client.fetchCompanyNews("AAPL",
                LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2));

        assertThat(result).isEmpty();
    }

    @Test
    void 서버_500_오류_예외_발생() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() ->
                client.fetchCompanyNews("AAPL",
                        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 2))
        ).isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void 요청_경로_및_쿼리_파라미터_검증() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("[]"));

        client.fetchCompanyNews("TSLA",
                LocalDate.of(2025, 3, 1), LocalDate.of(2025, 3, 31));

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getPath()).contains("/company-news");
        assertThat(request.getPath()).contains("symbol=TSLA");
        assertThat(request.getPath()).contains("from=2025-03-01");
        assertThat(request.getPath()).contains("to=2025-03-31");
        assertThat(request.getMethod()).isEqualTo("GET");
    }
}
