package com.stocknews.api.domain.stock;

import com.stocknews.api.common.response.ApiResponse;
import com.stocknews.api.domain.news.NewsService;
import com.stocknews.api.domain.sector.StockScreeningService;
import com.stocknews.api.domain.stock.dto.ScoreBreakdownResponse;
import com.stocknews.api.domain.stock.dto.SentimentTrendResponse;
import com.stocknews.api.domain.stock.dto.StockProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService          stockService;
    private final StockScreeningService stockScreeningService;
    private final NewsService           newsService;

    /**
     * GET /api/v1/stocks/{ticker}
     * 종목 기본정보 + 최신 재무지표 (캐시 6시간).
     */
    @GetMapping("/{ticker}")
    public ResponseEntity<ApiResponse<StockProfileResponse>> getStockProfile(
            @PathVariable String ticker
    ) {
        return ResponseEntity.ok(ApiResponse.success(stockService.getStockProfile(ticker)));
    }

    /**
     * GET /api/v1/stocks/{ticker}/score-breakdown
     * 성장주/대형주 스코어 팩터별 백분위 내역 (캐시 6시간, 배치 산출).
     */
    @GetMapping("/{ticker}/score-breakdown")
    public ResponseEntity<ApiResponse<ScoreBreakdownResponse>> getScoreBreakdown(
            @PathVariable String ticker
    ) {
        return ResponseEntity.ok(ApiResponse.success(stockScreeningService.getScoreBreakdown(ticker)));
    }

    /**
     * GET /api/v1/stocks/{ticker}/sentiment-trend
     * 최근 30일 일별 뉴스 감정 추이 (캐시 1시간).
     */
    @GetMapping("/{ticker}/sentiment-trend")
    public ResponseEntity<ApiResponse<SentimentTrendResponse>> getSentimentTrend(
            @PathVariable String ticker
    ) {
        return ResponseEntity.ok(ApiResponse.success(newsService.getSentimentTrend(ticker)));
    }
}
