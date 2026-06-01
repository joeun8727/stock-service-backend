# Stock Service API — 프로젝트 가이드

미국 주식(미장) 섹터·종목 분석 정보를 제공하는 백엔드 API. 1인 개발/운영.

> 📌 **API 계약은 루트 `../CLAUDE.md`의 "API 계약" 섹션이 기준입니다.** 엔드포인트/응답 스키마 변경 시 루트 파일과 프론트 타입을 함께 동기화하세요.

> ⚠️ **이 서비스는 투자 추천이 아닌 정보 제공 목적입니다.** 모든 응답에 면책 문구가 포함되어야 합니다. (`.claude/rules/08-compliance.md` 참조)

## 핵심 기능

1. **유망 섹터 랭킹** (1~5위) — 뉴스 볼륨 + 재무 + 거시지표 기반
2. **섹터별 종목 Top 20** — 대형주 / 성장주 두 가지 기준
3. **종목 기본정보 + 재무지표** — ROE, ROA, ROIC, PER, PBR 등
4. **종목별 뉴스 + LLM 분석** — Gemini 요약 + 감정/중요도 점수

## 데이터 흐름

```
유망 섹터 랭킹 (1~5위)
      ↓
섹터별 대형주 Top 20 / 성장주 Top 20 (재무지표)
      ↓
종목 기본정보 + 최신 뉴스 (Gemini 요약/감정분석)
```

## 규칙 파일 인덱스

작업 영역에 맞는 규칙을 반드시 먼저 읽고 작업하세요:

| 파일 | 언제 읽는가 |
|------|------------|
| `.claude/rules/tech-stack.md` | 모든 작업 (기술 스택, 코딩 컨벤션) |
| `.claude/rules/architecture.md` | 새 기능/클래스 추가 시 (레이어, 패키지 구조) |
| `.claude/rules/external-api.md` | 외부 API 클라이언트 작업 시 (Finnhub, FRED, Gemini) |
| `.claude/rules/data-model.md` | 엔티티/DB/마이그레이션 작업 시 |
| `.claude/rules/sector-ranking.md` | 섹터 랭킹 기능 작업 시 |
| `.claude/rules/stock-screening.md` | 종목 Top 20 스크리닝 작업 시 |
| `.claude/rules/news-llm.md` | 뉴스 수집/LLM 분석 작업 시 |
| `.claude/rules/caching.md` | Redis 캐싱 작업 시 |
| `.claude/rules/compliance.md` | API 응답 작업 시 (면책 문구 필수) |

## 작업 원칙

- **구체성 우선**: "적절히" 같은 모호한 표현 금지. 숫자/키/TTL을 명시.
- **한 번에 하나씩**: 한 Step을 완성하고 테스트한 뒤 다음으로.
- **외부 API는 항상 추상화**: 인터페이스 + 구현체 분리. (`02-external-api.md`)
- **모든 외부 호출에 Rate Limit + Circuit Breaker** 적용.
- **응답/주석/커밋 메시지는 한국어**로 작성.

## 빌드 & 실행

```bash
docker compose up -d        # MySQL + Redis 기동
./gradlew bootRun           # 앱 실행
./gradlew test              # 테스트
```
