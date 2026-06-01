# 05 — 섹터별 종목 Top 20 스크리닝

## 목표

선택한 섹터에서 종목을 **두 가지 기준**으로 Top 20 산출:
1. **대형주** (LARGE_CAP) — 시총 내림차순 상위 20
2. **성장주** (GROWTH) — 섹터 그룹별 가중치 스코어 상위 20

결과는 `StockScore` 테이블에 미리 계산·저장. **API는 읽기 전용** (실시간 계산·Finnhub 호출 금지).

---

## 종목 마스터 동적 구성 (StockMasterScheduler)

하드코딩된 종목 목록 없음. 매일 Finnhub `/stock/symbol?exchange=US` 로 미국 상장 전 종목을 수집:

1. Common Stock + USD 필터, 티커에 `.` 포함 종목 제외
2. `IndustryToSectorMapper` — Finnhub `finnhubIndustry` 문자열 → 10개 섹터 코드 매핑 (72개 매핑 정의)
3. 매핑 실패(null 반환) 종목은 건너뜀
4. 섹터별 시총 내림차순 상위 **100개**만 Stock 테이블에 Upsert (`ScoringWeights.STOCK_MASTER_TOP_N_PER_SECTOR`)
5. 스케줄: `scheduler.stock-master.cron` (기본 매일 04:30 KST, 재무지표 수집 30분 전)

---

## 섹터 그룹 분류 (SectorGroupConfig)

| 그룹 | 섹터 코드 |
|------|----------|
| GROWTH_TECH | AI_SOFTWARE, ROBOTICS, CYBERSECURITY, EV_BATTERY, AEROSPACE |
| TRADITIONAL | SEMICONDUCTOR, ENERGY, FINANCE, CONSUMER_GOODS, HEALTHCARE_BIO |

그룹에 따라 **스코어링 지표와 가중치가 다름** (ScoringWeights 참조).

---

## 대형주 (type=large_cap)

- **정렬 기준**: 시총 내림차순 상위 20 (부채비율 초과 종목 제외 후)
- 점수: `100 - (rank - 1) × 5` (1위=100, 20위=5)
- 지표 가중치 없음 — 시총 순서 자체가 순위

---

## 성장주 (type=growth)

### 하드 필터

| 조건 | 기준 |
|------|------|
| 부채비율 | `debtRatio > 2.0` 이면 제외 (`ScoringWeights.DEBT_RATIO_MAX`) |
| 대형주 중복 제외 | 대형주 Top 20에 포함된 종목은 성장주 풀에서 제외 |
| 시총 밴드 | $10B ~ $500B (`GROWTH_MARKET_CAP_MIN/MAX`) |
| 영업이익 | TRADITIONAL 그룹만 영업이익 음수 종목 제외. GROWTH_TECH는 허용 |

### 스코어링 지표 (섹터 내 백분위 기반)

#### GROWTH_TECH 가중치

| 지표 | 가중치 | 설명 |
|------|--------|------|
| Rule of 40 | 35% | `revenueGrowth% + max(fcfMargin, operatingMargin)%` |
| Gross Margin 기울기 | 25% | 최근 8분기 선형회귀 기울기 (MetricTrend) |
| ROIC 기울기 | 20% | 최근 8분기 선형회귀 기울기 (MetricTrend) |
| PEG | 20% | LOWER_BETTER. 음수(적자)→null→중립 50 처리 |

#### TRADITIONAL 가중치

| 지표 | 가중치 | 설명 |
|------|--------|------|
| 매출성장률 YoY | 40% | `revenueGrowthYoy` |
| 영업이익률 기울기 | 25% | 최근 8분기 선형회귀 기울기 (MetricTrend) |
| ROIC 기울기 | 20% | 최근 8분기 선형회귀 기울기 (MetricTrend) |
| PSR | 15% | LOWER_BETTER |

### 백분위 계산 규칙

- **절대값 비교 금지** — 반드시 같은 섹터 내 순위 기반 백분위 (0~100).
- 동순위 처리: 평균 순위 부여.
- LOWER_BETTER (peg, psr): `100 - ascending_percentile`.
- 지표값 null → **중립 50.0** (페널티 없음, `ScoringWeights.MISSING_PERCENTILE`).
- PEG 음수(적자) → null로 처리 (중립).

### 추세 계산 (TrendCalculator)

- Finnhub `series.quarterly` 데이터 사용 (최대 8분기).
- 최소 유효 포인트 3개 미만이면 기울기 계산 불가 → null → 중립.
- 상하위 10% 윈저화(winsorization) 적용.
- 시간 인덱스 보존 (null 포인트는 값만 제외, 위치는 유지) → 간격 있는 시계열 정확도 보장.
- 결과: `MetricTrend` 테이블 Upsert (metric_name: `grossMargin`, `roic`, `operatingMargin`, `fcfMargin`).

### 최종 산출

- 가중합 점수 내림차순 정렬 → 상위 20개가 성장주 Top 20.
- `Stock.is_growth_candidate = true` 플래그 갱신 (Top 20 이외는 false).
- 결과 `StockScore` 테이블 Upsert (`screen_type='GROWTH'`).

---

## 배치 실행 순서

```
[04:30 KST] StockMasterScheduler   — 종목 마스터 갱신
[05:00 KST] FinancialMetricsScheduler — 지표 수집 + TrendCalculator + ScoringService
[06:00 KST] SectorRankingScheduler — 섹터 랭킹 산출
```

`FinancialMetricsScheduler.collectFinancialMetrics()` 완료 후 자동으로 `ScoringService.scoreAllSectors()` 호출.

---

## 엔드포인트

```
GET /api/v1/sectors/{sectorId}/stocks?type=large_cap&limit=20
GET /api/v1/sectors/{sectorId}/stocks?type=growth&limit=20
```

- `type`: `large_cap` | `growth` (필수)
- `limit`: 1~50, 기본 20

응답 예시:
```json
{
  "success": true,
  "data": {
    "sectorId": 1,
    "sectorCode": "AI_SOFTWARE",
    "sectorName": "AI/소프트웨어",
    "type": "growth",
    "stocks": [
      {
        "rank": 1,
        "ticker": "XXXX",
        "companyName": "...",
        "marketCap": 45000000000,
        "metrics": {
          "roe": 0.18, "roa": 0.11, "roic": 0.15,
          "per": 32.4, "pbr": 4.1,
          "revenueGrowthYoy": 0.45, "operatingMargin": 0.22,
          "psr": 8.3, "peg": 1.2, "grossMargin": 0.71, "fcfMargin": 0.18
        },
        "growthScore": 81.50
      }
    ]
  },
  "disclaimer": "본 정보는 투자 추천이 아닙니다."
}
```

---

## 핵심 클래스 참조

| 클래스 | 역할 |
|--------|------|
| `StockMasterScheduler` | Finnhub 전 종목 수집 → Stock 테이블 갱신 |
| `IndustryToSectorMapper` | finnhubIndustry → sectorCode 매핑 |
| `SectorGroupConfig` | 섹터 → GROWTH_TECH / TRADITIONAL 분류 |
| `ScoringWeights` | 모든 가중치·임계값 상수 (코드 변경 없이 수치 튜닝 가능) |
| `TrendCalculator` | 선형회귀 기울기 계산 + MetricTrend Upsert |
| `ScoringService` | 섹터 내 백분위 스코어링 + StockScore Upsert |
| `StockScreeningService` | API 레이어 — StockScore 테이블 읽기 전용 |

## 규칙

- **StockScreeningService는 읽기 전용**: 점수 계산·Finnhub 호출 절대 금지. `StockScore` 조회만.
- 데이터 없는 종목은 결과에서 제외 (null 지표를 0으로 채우지 말 것 → 왜곡 방지). null → 중립 50 처리는 스코어링 단계에서만.
- 가중치 변경 시 `ScoringWeights.java`만 수정 — application.yml 불필요.
- `StockScore` 비어있을 때 API 응답: 빈 배열 반환 (에러 아님). 배치 미실행 상태로 간주.
