# 07 — Redis 캐싱 전략

## 원칙

- 자주 조회되고 자주 안 변하는 데이터는 캐싱.
- 외부 API 호출 결과는 적극 캐싱 (Rate Limit 보호 + 응답 속도).
- **성능 목표**: 단일 종목 조회 500ms 이하 (캐시 히트 기준).

## 캐시 키 & TTL (명시적)

| 데이터 | 키 패턴 | TTL |
|--------|---------|-----|
| 종목 기본정보 | `stock:{ticker}:profile` | 6시간 |
| 종목 재무지표 | `stock:{ticker}:metrics` | 1시간 |
| 종목 뉴스 목록(첫 페이지) | `stock:{ticker}:news:p0` | 30분 |
| 종목 LLM 요약 | `stock:{ticker}:summary` | 6시간 |
| 섹터 랭킹 | `sector:ranking` | 12시간 |
| 섹터별 Top20(대형) | `sector:{id}:largecap` | 6시간 |
| 섹터별 Top20(성장) | `sector:{id}:growth` | 6시간 |
| FRED 거시지표 | `macro:{seriesId}` | 24시간 |

## 구현

- Spring `@Cacheable` / `@CacheEvict` 활용 + `RedisCacheManager`.
- 직렬화: JSON (`GenericJackson2JsonRedisSerializer`).
- 키 패턴은 상수로 정의 (`CacheKeys` 클래스). 문자열 하드코딩 금지.

```java
@Cacheable(value = "stockProfile", key = "#ticker")
public StockProfileResponse getProfile(String ticker) { ... }
```

## 캐시 무효화

- 배치(스크리닝/랭킹) 갱신 후 관련 캐시 evict.
- 뉴스 신규 수집 시 `stock:{ticker}:news:*` evict.

## 규칙

- **캐시 미스 시에만 외부 API 호출.** 캐시 우선 조회를 Service 레벨에서 보장.
- 캐시 장애(Redis down) 시에도 서비스는 동작해야 함 → DB fallback, 예외로 전체 실패 금지.
- TTL은 위 표 값 사용. 임의 변경 시 이 파일도 함께 업데이트.
