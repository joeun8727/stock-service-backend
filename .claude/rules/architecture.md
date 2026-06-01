# 01 — 아키텍처 & 패키지 구조

## 레이어 구조 (Spring 표준)

```
Controller → Service → Repository → Entity
                ↓
         External Client (Finnhub, FRED, Gemini)
```

- **Controller**: HTTP 요청/응답만 담당. 비즈니스 로직 금지. DTO ↔ 도메인 변환만.
- **Service**: 비즈니스 로직. 트랜잭션 경계(`@Transactional`)는 여기서.
- **Repository**: JPA 데이터 접근만.
- **Client**: 외부 API 호출. 인터페이스로 추상화 (`02-external-api.md`).

## 패키지 구조

```
com.stocknews.api
├── StockNewsApplication.java
├── common/
│   ├── response/        # ApiResponse, ErrorInfo
│   ├── exception/       # BusinessException, ErrorCode, GlobalExceptionHandler
│   ├── config/          # RedisConfig, RestClientConfig, Resilience4jConfig
│   └── admin/           # AdminController (local/dev 수동 트리거)
├── domain/
│   ├── stock/           # Stock 엔티티 + Repository + Service + Controller
│   ├── sector/          # Sector, SectorController, SectorRankingService
│   │                    # StockScreeningService (읽기 전용)
│   │                    # ScoringService, TrendCalculator
│   │                    # SectorGroupConfig, ScoringWeights, IndustryToSectorMapper
│   │                    # StockScore + StockScoreRepository
│   ├── news/            # NewsArticle 관련
│   └── financial/       # FinancialMetric + FinancialMetricRepository
│                        # MetricTrend + MetricTrendRepository
├── client/
│   ├── news/            # NewsProvider 인터페이스 + FinnhubNewsClient
│   ├── financial/       # FinancialProvider + FinnhubFinancialClient
│   │                    # FinnhubMetricData, FinnhubMetricResponse
│   │                    # FinnhubSeriesData, SeriesPoint, FinnhubSymbolItem
│   │                    # RawFinancialMetric
│   ├── macro/           # MacroProvider + FredClient
│   └── llm/             # LLMClient + GeminiClient
└── scheduler/           # 폴링 잡 (@Scheduled)
                         # NewsCollectionScheduler, LlmAnalysisScheduler
                         # FinancialMetricsScheduler, SectorRankingScheduler
                         # StockMasterScheduler
```

## 도메인별 패키지 내부 구조 (예: stock)

```
domain/stock/
├── StockController.java
├── StockService.java
├── StockRepository.java
├── Stock.java                 # 엔티티
└── dto/
    ├── StockProfileResponse.java
    └── StockSummaryResponse.java
```

## 규칙

- **Controller는 절대 Client/Repository를 직접 호출하지 않음.** 반드시 Service 경유.
- **엔티티를 Controller 응답으로 직접 노출 금지.** 항상 DTO(record)로 변환.
- **순환 의존 금지**: domain 패키지 간 직접 참조 최소화. 필요 시 Service 레벨에서 조합.
- **Client는 도메인을 모름**: Client는 외부 API 응답 DTO만 다루고, 도메인 변환은 Service에서.
- **@Transactional은 Service 메서드에만.** 외부 API 호출을 트랜잭션 안에 넣지 말 것 (커넥션 점유 방지).

## API 버저닝

- 모든 엔드포인트는 `/api/v1/` 접두사.
- Phase 1은 **인증 없음** (오픈 API). 단, 추후 인증 추가 가능하도록 Controller에 보안 의존 하드코딩 금지.
