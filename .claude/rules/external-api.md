# 02 — 외부 API 클라이언트 추상화

## 핵심 원칙

**모든 외부 API는 인터페이스로 추상화한다.** 향후 API 교체/추가가 가능해야 함.

```
NewsProvider (interface)
  └── FinnhubNewsClient (impl)        # 나중에 NaverNewsClient 추가 가능

FinancialProvider (interface)
  └── FinnhubFinancialClient (impl)

MacroProvider (interface)
  └── FredClient (impl)

LLMClient (interface)
  └── GeminiClient (impl)             # 나중에 ClaudeClient 교체 가능
```

## 인터페이스 예시

```java
public interface NewsProvider {
    List<RawNews> fetchCompanyNews(String ticker, LocalDate from, LocalDate to);
}

public interface LLMClient {
    LLMAnalysis analyze(String content);  // 요약 + 감정 + 중요도
}
```

## 외부 데이터 소스 (Phase 1 — 모두 무료 티어)

| 소스 | 한도 | 용도 | 환경변수 |
|------|------|------|---------|
| **Finnhub** | 분당 60콜 | 기업 뉴스, 재무지표, 프로필 | `FINNHUB_API_KEY` |
| **FRED** | 사실상 무제한 | 거시지표 (금리, GDP 등) | `FRED_API_KEY` |
| **Gemini 2.5 Flash** | 분당 10, 일 250 | 뉴스 요약/감정분석 | `GEMINI_API_KEY` |

## Rate Limit 관리 (필수)

각 외부 API별 호출 한도를 추적하고 초과 방지:

- **Resilience4j RateLimiter** 사용. API별로 별도 인스턴스 구성.
  - `finnhub`: 분당 60 (안전하게 55로 설정)
  - `gemini`: 분당 10 (안전하게 9로 설정), 일 250 별도 카운터
- 한도 초과 시 **백오프 후 재시도** 또는 큐잉. 절대 무한 호출 금지.
- `ApiCallLog` 테이블에 호출 기록 (`03-data-model.md`).

## Circuit Breaker & Retry (Resilience4j)

모든 외부 Client 메서드에 적용:

```java
@CircuitBreaker(name = "finnhub", fallbackMethod = "fallbackNews")
@Retry(name = "finnhub")
@RateLimiter(name = "finnhub")
public List<RawNews> fetchCompanyNews(String ticker, ...) { ... }
```

설정 (`application.yml`):
- Circuit Breaker: 실패율 50% 초과 시 open, 30초 후 half-open
- Retry: 최대 3회, 지수 백오프 (1s, 2s, 4s)
- Fallback: 캐시된 데이터 반환 또는 빈 결과 + 로그

## HTTP Client

- Spring `RestClient` 사용 (동기). 타임아웃 명시: connect 3s, read 10s.
- 외부 API 응답은 전용 DTO(record)로 역직렬화. 도메인 엔티티 직접 매핑 금지.
- API 키는 절대 로그에 남기지 않음.

## 테스트

- 외부 API 호출은 **MockWebServer** 또는 Mockito로 모킹.
- 실제 API 키 없이 테스트 통과 가능해야 함.
