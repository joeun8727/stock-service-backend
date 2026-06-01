package com.stocknews.api.client.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.domain.apicalllog.ApiCallLog;
import com.stocknews.api.domain.apicalllog.ApiCallLogRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
public class GeminiClient implements LLMClient {

    private static final String PROVIDER = "gemini";
    private static final String ENDPOINT = "/models/{model}:generateContent";

    private final RestClient restClient;
    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final ApiCallLogRepository apiCallLogRepository;

    public GeminiClient(RestClient restClient, String apiKey, String model,
                        ObjectMapper objectMapper, ApiCallLogRepository apiCallLogRepository) {
        this.restClient = restClient;
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;
        this.apiCallLogRepository = apiCallLogRepository;
    }

    @Override
    @CircuitBreaker(name = "gemini", fallbackMethod = "fallbackAnalysis")
    @Retry(name = "gemini")
    @RateLimiter(name = "gemini")
    public LLMAnalysis analyze(String headline, String snippet) {
        String status = "SUCCESS";
        try {
            GeminiRequest request = GeminiRequest.of(buildPrompt(headline, snippet));

            GeminiResponse response = restClient.post()
                    .uri(u -> u.path(ENDPOINT)
                            .queryParam("key", apiKey)
                            .build(model))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null || response.candidates() == null || response.candidates().isEmpty()) {
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "Gemini 응답 후보 없음");
            }

            String rawText = response.candidates().get(0).content().parts().get(0).text();
            String jsonText = extractJson(rawText);
            GeminiLlmOutput output;
            try {
                output = objectMapper.readValue(jsonText, GeminiLlmOutput.class);
            } catch (JsonProcessingException e) {
                log.warn("Gemini LLM 응답 JSON 파싱 실패: {} | 원문: {}", e.getMessage(), rawText);
                throw new BusinessException(ErrorCode.EXTERNAL_API_ERROR, "LLM 응답 형식 오류");
            }

            return new LLMAnalysis(output.summary(), output.sentiment(), output.importance(), output.relevance()); // importance nullable
        } catch (RestClientException e) {
            status = "ERROR";
            throw e;
        } finally {
            saveLog(ENDPOINT, status);
        }
    }

    public LLMAnalysis fallbackAnalysis(String headline, String snippet, Throwable t) {
        log.warn("Gemini LLM 분석 fallback 실행 — 원인={}", t.getMessage());
        throw new BusinessException(ErrorCode.EXTERNAL_API_CIRCUIT_OPEN, "LLM 분석 서비스 일시 중단");
    }

    private String buildPrompt(String headline, String snippet) {
        return """
                다음 미국 주식 뉴스를 분석해 JSON으로만 응답하세요. 마크다운·추가 설명 절대 금지.
                투자 권유·매수/매도 표현 금지, 사실 기반 요약만 작성하세요.

                헤드라인: %s
                스니펫: %s

                [출력 기준]
                summary : 3줄 이내 한국어 요약
                sentiment: -1.0~1.0 소수 (긍정=양수, 부정=음수)
                importance: 0~100 정수 — 투자 관점 중대성
                  80~100: 실적 발표·M&A·대형 계약·규제 승인·소송 결과
                  50~79 : 신제품·가이던스 변경·파트너십·경영진 교체
                  20~49 : 일반 사업 소식·시장 코멘트
                  0~19  : 광고성·단순 시세 언급·중복 보도
                relevance: HIGH(직접 관련)/MEDIUM(간접)/LOW(무관)
                일관성 규칙: relevance=HIGH이면 importance>=60, MEDIUM이면 importance>=30

                [예시1 — 실적 발표]
                헤드라인: "NVIDIA Q4 Revenue Beats Estimates at $22.1B, Raises Guidance"
                스니펫: "NVIDIA reported record quarterly revenue driven by data center demand."
                응답: {"summary":"NVIDIA가 Q4 매출 221억 달러로 예상치를 상회하는 사상 최대 실적을 기록했습니다. 데이터센터 수요 급증이 성장을 견인했으며 다음 분기 가이던스도 상향됐습니다. 주가에 긍정적 영향이 예상됩니다.","sentiment":0.85,"importance":92,"relevance":"HIGH"}

                [예시2 — 일반 시장 코멘트]
                헤드라인: "Analysts See Tech Stocks as Attractive at Current Valuations"
                스니펫: "Multiple analysts issued sector-wide notes on tech valuations."
                응답: {"summary":"복수 애널리스트가 현재 밸류에이션 수준에서 기술주가 매력적이라 평가했습니다. 특정 종목 추천 없이 섹터 전반에 긍정적 시각을 유지했습니다. 단기보다 장기 관점의 분석입니다.","sentiment":0.30,"importance":22,"relevance":"LOW"}

                [응답 — JSON만]
                """.formatted(headline, snippet != null ? snippet : "");
    }

    // 마크다운 코드블록 제거 후 첫 번째 JSON 객체 추출
    private static String extractJson(String text) {
        String s = text.trim();
        // ```json ... ``` 또는 ``` ... ``` 형태 제거
        if (s.startsWith("```")) {
            s = s.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "").trim();
        }
        // 중괄호 시작/끝 위치로 JSON 범위 추출
        int start = s.indexOf('{');
        int end   = s.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return s.substring(start, end + 1);
        }
        return s;
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
