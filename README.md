# StockNews API — 백엔드

미국 주식(미장) 섹터·종목 분석 정보를 제공하는 REST API 서버.

> ⚠️ **본 서비스는 투자 추천이나 자문이 아닌 정보 제공만을 목적으로 합니다.**
> 모든 API 응답에 면책 문구가 포함됩니다.

---

## 기술 스택

| 항목 | 버전 |
|------|------|
| Java | 25 |
| Spring Boot | 4.x |
| DB | MySQL 8.4 |
| Cache | Redis 7.4 |
| ORM | Spring Data JPA + Hibernate |
| HTTP Client | Spring RestClient (동기) |
| Resilience | Resilience4j (Circuit Breaker / Retry / RateLimiter) |
| DB 마이그레이션 | Flyway |
| Build | Gradle (Kotlin DSL) |

---

## 서비스 구조

### 전체 데이터 흐름

```
외부 데이터 수집 (배치, @Scheduled)
  ├─ Finnhub API  → 뉴스, 재무지표, 기업 프로필
  ├─ FRED API     → 거시지표 (기준금리 등)
  └─ Gemini LLM   → 뉴스 요약 / 감정 분석

          ↓

    MySQL (영속 저장)
    Redis  (응답 캐싱)

          ↓

REST API (조회 전용 — 실시간 계산 없음)
```

### 레이어 구조

```
Controller
    └─ Service          ← 비즈니스 로직, 트랜잭션 경계
         ├─ Repository  ← JPA 데이터 접근
         └─ Client      ← 외부 API 추상화 (인터페이스 + 구현체)
```

### 패키지 구조

```
com.stocknews.api
├── common/
│   ├── config/          # Redis, RestClient, Resilience4j, CacheKeys
│   ├── exception/       # BusinessException, ErrorCode, GlobalExceptionHandler
│   └── response/        # ApiResponse<T>, ErrorInfo
├── client/
│   ├── news/            # NewsProvider ← FinnhubNewsClient
│   ├── financial/       # FinancialProvider ← FinnhubFinancialClient
│   ├── macro/           # MacroProvider ← FredClient
│   └── llm/             # LLMClient ← GeminiClient
├── domain/
│   ├── stock/           # Stock 엔티티 + 프로필 API
│   ├── sector/          # Sector + 랭킹 + 스크리닝
│   ├── financial/       # FinancialMetric + 재무지표 API
│   ├── news/            # NewsArticle + 뉴스 수집 파이프라인
│   └── apicalllog/      # API 호출 이력 (Rate Limit 추적)
└── scheduler/           # 3개 배치 스케줄러
```

### DB 스키마

```
Sector ──< Stock ──< FinancialMetric
                └──< NewsArticle
ApiCallLog  (외부 API 호출 이력)
```

---

## API 엔드포인트

Base URL: `http://localhost:8080/api/v1`

모든 응답은 공통 래퍼로 감싸 반환됩니다.

```json
{
  "success": true,
  "data": { ... },
  "disclaimer": "본 정보는 투자 추천이나 자문이 아니며...",
  "error": null,
  "timestamp": "2026-05-29T12:00:00Z"
}
```

### 섹터

| Method | Path | 설명 | 캐시 |
|--------|------|------|------|
| GET | `/sectors/ranking` | 유망 섹터 1~5위 | 12시간 |
| GET | `/sectors/{sectorId}/trend` | 섹터 14일 뉴스 볼륨·감정 추이 | - |
| GET | `/sectors/{sectorId}/stocks?type=large_cap&limit=20` | 시가총액 상위 20 | 6시간 |
| GET | `/sectors/{sectorId}/stocks?type=growth&limit=20` | 성장 점수 상위 20 | 6시간 |

### 종목

| Method | Path | 설명 | 캐시 |
|--------|------|------|------|
| GET | `/stocks/{ticker}` | 기본정보 + 최신 재무지표 | 6시간 |
| GET | `/stocks/{ticker}/financials?period=quarterly&limit=4` | 재무지표 시계열 | 1시간 |
| GET | `/stocks/{ticker}/news?page=0&size=20&minImportance=50` | 뉴스 목록 (페이징) | 30분 |
| GET | `/stocks/{ticker}/summary` | LLM 종합 요약 | 6시간 |

**Query Params**
- `period`: `quarterly` (기본) \| `annual`
- `limit`: 1~20 (기본 4)
- `type`: `large_cap` \| `growth`
- `minImportance`: 0~100, 중요도 하한 필터

---

## 섹터 랭킹 점수 산출 로직

배치(`@Scheduled`)가 하루 1회 산출 후 DB에 저장. API는 저장된 값만 조회합니다.

```
총점 = 뉴스 볼륨 추이 (25%)
     + 평균 감정 점수 (20%)
     + 평균 매출성장률 (25%)
     + 거시 연관성   (20%)   ← FRED 기준금리 기반 섹터별 매핑 테이블
     + 모멘텀        (10%)   ← 영업이익률 개선 종목 비율
```

데이터가 부족한 섹터는 순위에서 제외됩니다 (0점 처리 금지).

## 성장주 스크리닝 점수 산출 로직

```
성장 점수 = 매출성장률       (40%)   ← −20%→0점, +100%→100점 선형
          + ROIC 개선 추이   (25%)   ← 최근 3분기 개선 비율
          + 영업이익률 추세  (20%)   ← 최근 3분기 개선 비율
          + 시총 성장여력    (15%)   ← (1 − 시총/500B) × 100
```

시가총액 $500B 초과 종목 및 매출성장률 데이터 없는 종목은 제외됩니다.

---

## 스케줄링

모든 시각은 KST 기준입니다.

| 스케줄러 | 주기 | 동작 |
|---------|------|------|
| `FinancialMetricsScheduler` | 매일 **05:00 KST** | Finnhub에서 모든 US 종목 프로필·재무지표 수집 → DB 저장 → 성장주 후보(`is_growth_candidate`) 플래그 갱신 → 섹터 스크리닝 캐시 무효화 |
| `SectorRankingScheduler` | 매일 **06:00 KST** | 5개 점수 컴포넌트 산출 → Sector 테이블 갱신 → 섹터 랭킹 캐시 무효화 |
| `NewsCollectionScheduler` | **매 정시** (1h 주기) | Finnhub 뉴스 수집 → 키워드 필터 → DB 저장 → Gemini LLM 분석 배치 처리 |

### 뉴스 수집 파이프라인 (매 정시)

```
① Finnhub /company-news 폴링 (모든 US 종목)
        ↓
② source_url 기준 중복 제거
        ↓
③ 키워드 룰 기반 1차 필터 (통과율 목표 30~50%)
   → 설정: news.keywords.include (application.yml)
        ↓
④ llm_processed=false 로 DB 저장 (원문 본문 저장 금지)
        ↓
⑤ Gemini 2.5 Flash LLM 분석 배치
   → 일일 잔여 호출량 확인 (250건 한도)
   → Rate Limit 초과 시 다음 주기에 재처리 (llm_processed=false 유지)
```

### Resilience4j 설정 요약

| 항목 | Finnhub | Gemini | FRED |
|------|---------|--------|------|
| Circuit Breaker | 실패율 50% → OPEN, 30초 후 half-open | 동일 | 실패율 50% → OPEN, 60초 |
| Retry | 최대 3회, 지수 백오프 (1s→2s→4s) | 최대 3회, 2s→4s→8s | 최대 3회, 1s |
| Rate Limiter | **55 req/min** | **8 req/min** | 제한 없음 |

---

## 실행 방법

### 사전 요구사항

- Java 25
- Docker & Docker Compose
- Finnhub / FRED / Google Gemini API 키

### 1. 환경변수 설정

```bash
cp .env.example .env
```

`.env` 파일을 열고 실제 API 키를 입력합니다.

```env
# DB (Docker Compose용)
MYSQL_ROOT_PASSWORD=<your_root_password>
MYSQL_DATABASE=stocknews
MYSQL_USER=stocknews
MYSQL_PASSWORD=<your_db_password>

# 외부 API 키
FINNHUB_API_KEY=<your_finnhub_key>
FRED_API_KEY=<your_fred_key>
GEMINI_API_KEY=<your_gemini_key>
```

> API 키 발급:
> - Finnhub: https://finnhub.io (무료 플랜, 분당 60건)
> - FRED: https://fred.stlouisfed.org/docs/api/api_key.html (무료)
> - Gemini: https://aistudio.google.com (무료 플랜, 분당 10건 / 일 250건)

### 2-A. Docker Compose로 전체 실행 (권장)

```bash
# 전체 스택 빌드 + 기동 (MySQL → Redis → API 순서로 healthcheck 후 시작)
docker compose up -d --build
```

```bash
# 로그 확인
docker compose logs -f api

# 중지
docker compose down
```

| 컨테이너 | 포트 | 역할 |
|---------|------|------|
| `stocknews-mysql` | 3306 | MySQL 8.4 |
| `stocknews-redis` | 6379 | Redis 7.4 |
| `stocknews-api` | **8080** | Spring Boot API |

> MySQL healthcheck 통과 후 API가 기동되므로 최초 시작 시 약 60~90초 소요됩니다.

### 2-B. 로컬 개발 (인프라만 Docker, 앱은 직접 실행)

```bash
# MySQL + Redis만 기동
docker compose up -d mysql redis

# 앱 직접 실행 (핫리로드 가능)
./gradlew bootRun
```

기본 포트: **8080**

### 3. 동작 확인

```bash
# 헬스 체크
curl http://localhost:8080/actuator/health

# 섹터 랭킹
curl http://localhost:8080/api/v1/sectors/ranking

# 종목 기본정보
curl http://localhost:8080/api/v1/stocks/AAPL
```

> **참고**: 스케줄러가 최초 실행(05:00 / 06:00 KST)되기 전까지는 재무지표와 섹터 랭킹 데이터가 없습니다.
> 초기 데이터는 스케줄러 실행 후 자동으로 채워집니다.

### 4. 테스트 실행

```bash
./gradlew test
```

외부 API 없이 실행 가능합니다 (MockWebServer / Mockito로 모킹).

---

## Redis 캐시 TTL

| 캐시 키 패턴 | TTL |
|-------------|-----|
| `stockProfile::<TICKER>` | 6시간 |
| `stockMetrics::<TICKER>_*` | 1시간 |
| `stockNews::*` | 30분 |
| `stockSummary::<TICKER>` | 6시간 |
| `sectorRanking::all` | 12시간 |
| `sectorLargecap::<ID>_*` | 6시간 |
| `sectorGrowth::<ID>_*` | 6시간 |
| `macro::<SERIES_ID>` | 24시간 |

---

## 주요 설정 파일

| 파일 | 용도 |
|------|------|
| `src/main/resources/application.yml` | 전체 애플리케이션 설정 |
| `src/main/resources/db/migration/V1__init.sql` | DB 스키마 초기화 (Flyway) |
| `docker-compose.yml` | MySQL + Redis 컨테이너 정의 |
| `.env.example` | 환경변수 템플릿 |

---

## 컴플라이언스

- 모든 API 응답에 면책 문구(`disclaimer`) 포함
- 뉴스 원문 본문 저장·제공 금지 (헤드라인 + LLM 요약 + 출처 링크만)
- 투자 권유 표현 금지 (LLM 프롬프트에 명시)
- 외부 API 키 로그·응답 노출 금지

---

## 라이선스

본 프로젝트는 MIT License를 따릅니다.

---

## 면책 및 이용 안내 (Disclaimer)

본 저장소는 개인 학습·포트폴리오 목적의 예제 코드이며, 실제 운영 중인
서비스가 아닙니다.

- 본 프로젝트는 투자 추천·자문을 제공하지 않으며, 어떠한 금융 정보도 실제로
  서비스하지 않습니다. 코드는 구현 방식을 보여주기 위한 것입니다.
- 외부 API(Finnhub, FRED, Google Gemini)를 연동하는 코드가 포함되어 있으나,
  각 API의 데이터·서비스를 재배포하지 않습니다. 본 코드를 실행·이용할 경우,
  각 제공처의 이용약관(ToS)을 직접 확인하고 준수하는 것은 이용자 본인의
  책임입니다.
  - Finnhub: https://finnhub.io/terms-of-service
  - FRED: https://fred.stlouisfed.org/legal/
  - Google Gemini: https://policies.google.com/terms
- API 키 등 민감 정보는 포함되어 있지 않으며, 이용자가 각자 발급받아 .env로
  주입해야 합니다.
- 본 코드의 사용으로 발생하는 모든 결과에 대한 책임은 이용자에게 있습니다.
