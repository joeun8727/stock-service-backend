# 06 — 뉴스 수집 & LLM 분석 (Gemini)

## 파이프라인

```
[NewsCollectionScheduler — 매 정시]
Finnhub /company-news 폴링
      ↓
중복 제거 (source_url 기준)
      ↓
1차 키워드 룰 필터링  ← LLM 비용 절감 핵심
      ↓
NewsArticle 저장 (llm_processed=false)

[LlmAnalysisScheduler — 분당 1회, 최대 8건]
llm_processed=false 기사 큐에서 8건 꺼냄
      ↓
Gemini 2.5 Flash Lite 분석 (요약 + 감정 + 중요도)
  - 429 수신 시: Retry 30s 대기 후 1회 재시도
  - 일일 한도(1,000) 소진 시: 당일 중단 → 익일 재개
      ↓
NewsArticle 갱신 (llm_processed=true)
```

## 1단계: 수집

- **소스**: Finnhub `/company-news?symbol={ticker}&from=&to=`
- **대상**: 섹터 Top 20 (대형주 + 성장주) 종목들.
- **주기**: `@Scheduled` 1시간마다. 종목당 최근 1일치 수집.
- **중복 방지**: `source_url` 유니크 제약 + 저장 전 존재 확인.

## 2단계: 키워드 1차 필터링 (필수 — 비용 절감)

LLM 호출 **전에** 룰 기반으로 거름. 통과한 뉴스만 LLM에 보냄:

- **통과 키워드** (대소문자 무시): earnings, revenue, guidance, acquisition,
  merger, M&A, partnership, contract, FDA, approval, launch, lawsuit,
  regulation, downgrade, upgrade, layoff, expansion 등.
- **제외**: 단순 시세 리포트, 광고성, 동일 내용 반복.
- 필터 키워드는 설정 파일/상수로 분리해 조정 가능하게.

> 이 단계로 Gemini 무료 한도(일 1,500)를 보호. 통과율 목표 30~50%.

## 3단계: Gemini LLM 분석

`LLMClient` 인터페이스 경유 (`external-api.md`). 구현체 `GeminiClient`.

- **모델**: Google Gemini 2.5 Flash Lite (무료 티어).
- **출력 (JSON 강제)**:
  ```json
  {
    "summary": "3줄 이내 한국어 요약",
    "sentiment": -1.0 ~ 1.0,
    "importance": 75,
    "relevance": "HIGH | MEDIUM | LOW"
  }
  ```
- **importance 기준**:
  - 80~100: 실적 발표·M&A·대형 계약·규제 승인·소송 결과
  - 50~79 : 신제품·가이던스 변경·파트너십·경영진 교체
  - 20~49 : 일반 사업 소식·시장 코멘트
  - 0~19  : 광고성·단순 시세 언급·중복 보도
- **일관성 규칙**: relevance=HIGH이면 importance≥60, MEDIUM이면 importance≥30. 위반 시 llm_processed=false 유지 후 재처리.
- 프롬프트에 기준 구간·일관성 규칙·few-shot 예시 2개 포함. "JSON만 반환, 마크다운 금지" 명시.
- **입력**: 헤드라인 + Finnhub snippet (인메모리 큐 경유, DB 저장 금지). snippet 없는 기사는 headline만으로 fallback.
- Rate Limit: 분당 8 (실제 한도 10 RPM, 안전 마진 2), 일 1,000. 초과 시 큐잉 후 다음 주기 처리.

## 4단계: 저장

- `NewsArticle`에 **링크 + 요약 + 메타데이터만** 저장.
- **원문 본문 저장 절대 금지** (`compliance.md`).
- `llm_processed` 플래그로 미처리 건 추적.

## 엔드포인트

```
GET /api/v1/stocks/{ticker}/news?page=0&size=20&from=2026-01-01&to=2026-05-29&minImportance=50
```

- 페이징, 기간 필터, 최소 중요도 필터 지원.
- 정렬: `published_at` 내림차순 (기본) 또는 `importance` 순.

응답:
```json
{
  "data": {
    "ticker": "AAPL",
    "news": [
      {
        "headline": "...",
        "source": "Reuters",
        "sourceUrl": "https://...",
        "publishedAt": "...",
        "summary": "LLM 3줄 요약",
        "sentiment": 0.6,
        "importance": 78,
        "relevance": "HIGH"
      }
    ]
  },
  "disclaimer": "본 정보는 투자 추천이 아닙니다. 원문은 출처 링크를 확인하세요."
}
```

## LLM 추상화 규칙

- `LLMClient` 인터페이스로 Gemini/Claude/Local 교체 가능하게.
- 무료 한도 초과 시 대응: ① 키워드 필터 강화 ② 처리 큐 적체 → 다음 주기 ③ 향후 유료 전환 시 `ClaudeClient` 추가.
- LLM 응답 파싱 실패 시 해당 뉴스는 `llm_processed=false`로 두고 재시도 큐로.

## 종목 LLM 요약 (선택 기능)

종목 단위 요약/감정 종합:
```
GET /api/v1/stocks/{ticker}/summary
```
- 최근 N개 뉴스의 감정·중요도를 종합한 한 줄 코멘트 + 평균 감정.
- 캐싱 (TTL 6시간).
