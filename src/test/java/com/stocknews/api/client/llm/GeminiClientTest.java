package com.stocknews.api.client.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.domain.apicalllog.ApiCallLogRepository;
import com.stocknews.api.support.ClientTestSupport;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.HttpServerErrorException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class GeminiClientTest extends ClientTestSupport {

    GeminiClient client;

    @BeforeEach
    void setUp() {
        ApiCallLogRepository logRepository = mock(ApiCallLogRepository.class);
        client = new GeminiClient(testRestClient, "test-key", "gemini-2.0-flash",
                new ObjectMapper(), logRepository);
    }

    @Test
    void LLM_분석_정상_응답_파싱() {
        // Gemini 응답 내 text 필드는 JSON 문자열 (이스케이프 포함)
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"candidates":[{
                          "content":{"parts":[{
                            "text":"{\\"summary\\":\\"애플 1분기 실적이 시장 예상을 상회했습니다.\\",\\"sentiment\\":0.7,\\"importance\\":80,\\"relevance\\":\\"HIGH\\"}"
                          }]},
                          "finishReason":"STOP"
                        }]}
                        """));

        LLMAnalysis result = client.analyze("AAPL Q1 Earnings Beat", "Apple beat Q1.");

        assertThat(result.summary()).isEqualTo("애플 1분기 실적이 시장 예상을 상회했습니다.");
        assertThat(result.sentiment()).isEqualByComparingTo(new BigDecimal("0.7"));
        assertThat(result.importance()).isEqualTo(80);
        assertThat(result.relevance()).isEqualTo("HIGH");
    }

    @Test
    void LLM_응답_JSON_파싱_실패_예외() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"candidates":[{
                          "content":{"parts":[{"text":"not valid json at all"}]},
                          "finishReason":"STOP"
                        }]}
                        """));

        assertThatThrownBy(() -> client.analyze("headline", "snippet"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void 서버_오류_예외_발생() {
        mockWebServer.enqueue(new MockResponse().setResponseCode(500));

        assertThatThrownBy(() -> client.analyze("headline", "snippet"))
                .isInstanceOf(HttpServerErrorException.class);
    }

    @Test
    void 요청_경로_및_메서드_검증() throws Exception {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody("""
                        {"candidates":[{
                          "content":{"parts":[{
                            "text":"{\\"summary\\":\\"테스트\\",\\"sentiment\\":0.0,\\"importance\\":50,\\"relevance\\":\\"LOW\\"}"
                          }]}
                        }]}
                        """));

        client.analyze("test", "test");

        RecordedRequest request = mockWebServer.takeRequest();
        assertThat(request.getMethod()).isEqualTo("POST");
        assertThat(request.getPath()).contains("/models/gemini-2.0-flash:generateContent");
        assertThat(request.getPath()).contains("key=test-key");
    }
}
