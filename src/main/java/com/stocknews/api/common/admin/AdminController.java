package com.stocknews.api.common.admin;

import com.stocknews.api.domain.sector.ScoringService;
import com.stocknews.api.scheduler.FinancialMetricsScheduler;
import com.stocknews.api.scheduler.LlmAnalysisScheduler;
import com.stocknews.api.scheduler.NewsCollectionScheduler;
import com.stocknews.api.scheduler.SectorRankingScheduler;
import com.stocknews.api.scheduler.StockMasterScheduler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개발/테스트용 스케줄러 수동 트리거 — local/dev 프로파일에서만 활성화.
 */
@RestController
@RequestMapping("/api/v1/admin/trigger")
@RequiredArgsConstructor
@Profile({"local", "dev", "default"})
public class AdminController {

    private final NewsCollectionScheduler    newsCollectionScheduler;
    private final LlmAnalysisScheduler       llmAnalysisScheduler;
    private final FinancialMetricsScheduler  financialMetricsScheduler;
    private final SectorRankingScheduler     sectorRankingScheduler;
    private final StockMasterScheduler       stockMasterScheduler;
    private final ScoringService             scoringService;

    @PostMapping("/news")
    public ResponseEntity<String> triggerNews() {
        newsCollectionScheduler.collectNews();
        return ResponseEntity.ok("뉴스 수집 스케줄러 실행 완료");
    }

    @PostMapping("/llm")
    public ResponseEntity<String> triggerLlm() {
        llmAnalysisScheduler.processLlmBatch();
        return ResponseEntity.ok("LLM 분석 배치 실행 완료");
    }

    @PostMapping("/financial-metrics")
    public ResponseEntity<String> triggerFinancialMetrics() {
        financialMetricsScheduler.collectFinancialMetrics();
        return ResponseEntity.ok("재무지표 수집 스케줄러 실행 완료");
    }

    @PostMapping("/financial-metrics/missing")
    public ResponseEntity<String> triggerMissingMetrics() {
        financialMetricsScheduler.collectMissingMetrics();
        return ResponseEntity.ok("미수집 재무지표 수집 완료");
    }

    @PostMapping("/sector-ranking")
    public ResponseEntity<String> triggerSectorRanking() {
        sectorRankingScheduler.calculateRanking();
        return ResponseEntity.ok("섹터 랭킹 산출 스케줄러 실행 완료");
    }

    @PostMapping("/stock-master")
    public ResponseEntity<String> triggerStockMaster() {
        stockMasterScheduler.collectStockMaster();
        return ResponseEntity.ok("종목 마스터 수집 완료");
    }

    @PostMapping("/scoring")
    public ResponseEntity<String> triggerScoring() {
        scoringService.scoreAllSectors();
        return ResponseEntity.ok("섹터 스코어링 완료");
    }
}
