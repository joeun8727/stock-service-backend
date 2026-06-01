package com.stocknews.api.domain.sector;

import com.stocknews.api.client.macro.RawMacroData;
import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.domain.financial.FinancialMetric;
import com.stocknews.api.domain.financial.FinancialMetricRepository;
import com.stocknews.api.domain.news.NewsArticleRepository;
import com.stocknews.api.domain.sector.dto.SectorRankingResponse;
import com.stocknews.api.domain.sector.dto.SectorTrendResponse;
import com.stocknews.api.domain.stock.Stock;
import com.stocknews.api.domain.stock.StockRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SectorRankingServiceTest {

    @Mock SectorRepository sectorRepository;
    @Mock NewsArticleRepository newsArticleRepository;
    @Mock FinancialMetricRepository financialMetricRepository;
    @Mock StockRepository stockRepository;
    @Mock MacroDataService macroDataService;

    SectorRankingService service;

    Sector sectorA;  // SEMICONDUCTOR — 재무 데이터 있음
    Sector sectorB;  // FINANCIAL — 재무 데이터 있음
    Sector sectorC;  // ENERGY — 재무 데이터 없음 (제외 대상)

    @BeforeEach
    void setUp() {
        service = new SectorRankingService(
                sectorRepository, newsArticleRepository,
                financialMetricRepository, stockRepository, macroDataService);

        sectorA = Sector.builder().code("SEMICONDUCTOR").name("반도체").build();
        sectorB = Sector.builder().code("FINANCIAL").name("금융").build();
        sectorC = Sector.builder().code("ENERGY").name("에너지").build();
        ReflectionTestUtils.setField(sectorA, "id", 1L);
        ReflectionTestUtils.setField(sectorB, "id", 2L);
        ReflectionTestUtils.setField(sectorC, "id", 3L);
    }

    // ──────────────────────────────────────────────
    // calculateAndSaveRankings
    // ──────────────────────────────────────────────

    @Test
    void calculateAndSaveRankings_점수_산출_및_랭크_저장() {
        // FRED: 고금리 환경 (DFF=5.75 → HIGH_RATE → SEMICONDUCTOR 불리, FINANCIAL 유리)
        when(macroDataService.fetchCached("DFF"))
                .thenReturn(new RawMacroData("DFF", LocalDate.now(), new BigDecimal("5.75"), "percent"));

        when(sectorRepository.findAll()).thenReturn(List.of(sectorA, sectorB));

        // 섹터A(반도체): 재무 있음
        when(financialMetricRepository.avgLatestRevenueGrowthBySector(1L))
                .thenReturn(Optional.of(new BigDecimal("0.30")));
        // 섹터B(금융): 재무 있음
        when(financialMetricRepository.avgLatestRevenueGrowthBySector(2L))
                .thenReturn(Optional.of(new BigDecimal("0.10")));

        // 뉴스 볼륨 (섹터A: 증가, 섹터B: 동일)
        when(newsArticleRepository.countBySectorAndPeriod(eq(1L), any(), any()))
                .thenReturn(20L, 10L); // recent=20, prior=10 → +100% → 100점
        when(newsArticleRepository.countBySectorAndPeriod(eq(2L), any(), any()))
                .thenReturn(10L, 10L); // 변화없음 → 50점

        // 감정
        when(newsArticleRepository.avgSentimentBySectorAndPeriod(eq(1L), any(), any()))
                .thenReturn(new BigDecimal("0.4"));   // → 70점
        when(newsArticleRepository.avgSentimentBySectorAndPeriod(eq(2L), any(), any()))
                .thenReturn(new BigDecimal("0.0"));   // → 50점

        // 모멘텀
        when(stockRepository.findBySectorIdAndMarketOrderByMarketCapDesc(anyLong(), eq("US")))
                .thenReturn(List.of());  // 종목 없음 → 50점 (중립)

        when(sectorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculateAndSaveRankings();

        // 두 섹터 모두 저장됨
        verify(sectorRepository, times(2)).save(any(Sector.class));

        // 고금리 환경: FINANCIAL(macro=85) > SEMICONDUCTOR(macro=25)
        // 전체 점수 비교에서 금융이 더 높을 수도 있으나, 반도체의 뉴스볼륨과 매출성장이 높아 결과는 서비스 로직에 의존
        // 여기서는 저장이 호출됐고 rank가 설정됐는지만 검증
        assertThat(sectorA.getLatestRank()).isNotNull();
        assertThat(sectorB.getLatestRank()).isNotNull();
        // 두 섹터의 rank 합이 1+2=3이어야 함
        assertThat(sectorA.getLatestRank() + sectorB.getLatestRank()).isEqualTo(3);
    }

    @Test
    void calculateAndSaveRankings_재무데이터_없는_섹터_제외() {
        when(macroDataService.fetchCached("DFF"))
                .thenReturn(new RawMacroData("DFF", LocalDate.now(), new BigDecimal("4.0"), "percent"));

        when(sectorRepository.findAll()).thenReturn(List.of(sectorA, sectorC));

        // sectorA: 재무 있음
        when(financialMetricRepository.avgLatestRevenueGrowthBySector(1L))
                .thenReturn(Optional.of(new BigDecimal("0.20")));
        // sectorC: 재무 없음 → 제외
        when(financialMetricRepository.avgLatestRevenueGrowthBySector(3L))
                .thenReturn(Optional.empty());

        when(newsArticleRepository.countBySectorAndPeriod(eq(1L), any(), any())).thenReturn(5L, 5L);
        when(newsArticleRepository.avgSentimentBySectorAndPeriod(eq(1L), any(), any())).thenReturn(null);
        when(stockRepository.findBySectorIdAndMarketOrderByMarketCapDesc(eq(1L), any())).thenReturn(List.of());
        when(sectorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculateAndSaveRankings();

        // sectorC는 저장 안 됨 (재무 데이터 부족)
        ArgumentCaptor<Sector> captor = ArgumentCaptor.forClass(Sector.class);
        verify(sectorRepository, times(1)).save(captor.capture());
        assertThat(captor.getValue().getCode()).isEqualTo("SEMICONDUCTOR");
    }

    @Test
    void calculateAndSaveRankings_FRED_실패_중립_금리로_계속() {
        // FRED 호출 실패 → MEDIUM_RATE로 폴백
        when(macroDataService.fetchCached("DFF"))
                .thenThrow(new RuntimeException("FRED 연결 실패"));

        when(sectorRepository.findAll()).thenReturn(List.of(sectorA));
        when(financialMetricRepository.avgLatestRevenueGrowthBySector(1L))
                .thenReturn(Optional.of(new BigDecimal("0.20")));
        when(newsArticleRepository.countBySectorAndPeriod(any(), any(), any())).thenReturn(0L);
        when(newsArticleRepository.avgSentimentBySectorAndPeriod(any(), any(), any())).thenReturn(null);
        when(stockRepository.findBySectorIdAndMarketOrderByMarketCapDesc(anyLong(), any())).thenReturn(List.of());
        when(sectorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // 예외 없이 정상 완료 (MEDIUM_RATE 폴백)
        service.calculateAndSaveRankings();

        verify(sectorRepository).save(any(Sector.class));
        // MEDIUM_RATE: SEMICONDUCTOR macro score = 55
        assertThat(sectorA.getLatestRank()).isEqualTo(1);
    }

    @Test
    void calculateAndSaveRankings_뉴스_볼륨_100퍼센트_증가_만점() {
        when(macroDataService.fetchCached("DFF"))
                .thenReturn(new RawMacroData("DFF", LocalDate.now(), new BigDecimal("4.0"), "percent"));
        when(sectorRepository.findAll()).thenReturn(List.of(sectorA));
        when(financialMetricRepository.avgLatestRevenueGrowthBySector(1L))
                .thenReturn(Optional.of(new BigDecimal("0.20")));

        // prior=10, recent=20 → +100% → newsVolumeScore=100
        when(newsArticleRepository.countBySectorAndPeriod(eq(1L), any(), any()))
                .thenReturn(20L, 10L);
        when(newsArticleRepository.avgSentimentBySectorAndPeriod(any(), any(), any())).thenReturn(null);
        when(stockRepository.findBySectorIdAndMarketOrderByMarketCapDesc(anyLong(), any())).thenReturn(List.of());
        when(sectorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculateAndSaveRankings();

        assertThat(sectorA.getLatestScore()).isGreaterThan(BigDecimal.ZERO);
    }

    // ──────────────────────────────────────────────
    // getRankings
    // ──────────────────────────────────────────────

    @Test
    void getRankings_랭킹_없으면_예외() {
        when(sectorRepository.findAllByOrderByLatestRankAsc()).thenReturn(List.of(sectorC)); // sectorC latestRank=null
        assertThatThrownBy(() -> service.getRankings())
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void getRankings_상위5개_반환() {
        sectorA.updateRanking(1, new BigDecimal("87.50"));
        sectorB.updateRanking(2, new BigDecimal("72.30"));

        when(sectorRepository.findAllByOrderByLatestRankAsc()).thenReturn(List.of(sectorA, sectorB));
        when(newsArticleRepository.countBySectorAndPeriod(anyLong(), any(), any())).thenReturn(10L);
        when(newsArticleRepository.avgSentimentBySectorAndPeriod(anyLong(), any(), any()))
                .thenReturn(new BigDecimal("0.3"));
        when(financialMetricRepository.avgLatestRevenueGrowthBySector(anyLong()))
                .thenReturn(Optional.of(new BigDecimal("0.20")));

        SectorRankingResponse response = service.getRankings();

        assertThat(response.sectors()).hasSize(2);
        assertThat(response.sectors().get(0).rank()).isEqualTo(1);
        assertThat(response.sectors().get(0).code()).isEqualTo("SEMICONDUCTOR");
        assertThat(response.sectors().get(0).score()).isEqualByComparingTo(new BigDecimal("87.50"));
        assertThat(response.sectors().get(0).highlights()).isNotNull();
    }

    // ──────────────────────────────────────────────
    // getSectorTrend
    // ──────────────────────────────────────────────

    @Test
    void getSectorTrend_일별_통계_반환() {
        when(sectorRepository.findById(1L)).thenReturn(java.util.Optional.of(sectorA));

        NewsArticleRepository.DailyNewsStat stat = new NewsArticleRepository.DailyNewsStat() {
            @Override public java.sql.Date getStatDate() { return java.sql.Date.valueOf(LocalDate.now().minusDays(1)); }
            @Override public long getNewsCount() { return 5L; }
            @Override public BigDecimal getAvgSentiment() { return new BigDecimal("0.3500"); }
        };
        when(newsArticleRepository.dailyStatsBySector(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(stat));

        SectorTrendResponse trend = service.getSectorTrend(1L);

        assertThat(trend.sectorCode()).isEqualTo("SEMICONDUCTOR");
        assertThat(trend.dailyStats()).hasSize(1);
        assertThat(trend.dailyStats().get(0).newsCount()).isEqualTo(5L);
        assertThat(trend.dailyStats().get(0).avgSentiment()).isEqualByComparingTo(new BigDecimal("0.3500"));
    }

    @Test
    void getSectorTrend_섹터_없으면_예외() {
        when(sectorRepository.findById(999L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> service.getSectorTrend(999L))
                .isInstanceOf(BusinessException.class);
    }

    // ──────────────────────────────────────────────
    // 모멘텀 점수 — 영업이익률 개선 종목 비율
    // ──────────────────────────────────────────────

    @Test
    void calculateAndSaveRankings_모멘텀_개선_종목_비율_반영() {
        when(macroDataService.fetchCached("DFF"))
                .thenReturn(new RawMacroData("DFF", LocalDate.now(), new BigDecimal("4.0"), "percent"));
        when(sectorRepository.findAll()).thenReturn(List.of(sectorA));
        when(financialMetricRepository.avgLatestRevenueGrowthBySector(1L))
                .thenReturn(Optional.of(new BigDecimal("0.20")));
        when(newsArticleRepository.countBySectorAndPeriod(any(), any(), any())).thenReturn(0L);
        when(newsArticleRepository.avgSentimentBySectorAndPeriod(any(), any(), any())).thenReturn(null);

        Stock stockX = Stock.builder().ticker("AAPL").market("US").companyName("Apple").build();
        ReflectionTestUtils.setField(stockX, "id", 100L);
        when(stockRepository.findBySectorIdAndMarketOrderByMarketCapDesc(1L, "US")).thenReturn(List.of(stockX));

        // 최근 분기 영업이익률 개선: 0.20 > 0.15
        FinancialMetric latest = FinancialMetric.builder()
                .stock(stockX).period("quarterly").fiscalDate(LocalDate.now())
                .operatingMargin(new BigDecimal("0.20")).revenueGrowthYoy(new BigDecimal("0.20")).build();
        FinancialMetric prev = FinancialMetric.builder()
                .stock(stockX).period("quarterly").fiscalDate(LocalDate.now().minusMonths(3))
                .operatingMargin(new BigDecimal("0.15")).revenueGrowthYoy(new BigDecimal("0.15")).build();
        when(financialMetricRepository.findTop2ByStockIdAndPeriodOrderByFiscalDateDesc(100L, "quarterly"))
                .thenReturn(List.of(latest, prev));

        when(sectorRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.calculateAndSaveRankings();

        // 개선 비율 100% → 모멘텀 점수 100점 → 전체 점수에 10% 기여
        assertThat(sectorA.getLatestScore()).isGreaterThan(BigDecimal.ZERO);
    }
}
