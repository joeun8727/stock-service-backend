package com.stocknews.api.domain.news;

import com.stocknews.api.common.response.ApiResponse;
import com.stocknews.api.domain.news.dto.NewsPageResponse;
import com.stocknews.api.domain.stock.dto.StockSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class NewsController {

    private final NewsService newsService;

    /**
     * GET /api/v1/stocks/{ticker}/news
     * 종목 뉴스 목록 (페이지네이션 + 필터).
     */
    @GetMapping("/{ticker}/news")
    public ResponseEntity<ApiResponse<NewsPageResponse>> getNews(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Integer minImportance
    ) {
        int safeSize = Math.min(size, 50); // 최대 50건
        return ResponseEntity.ok(ApiResponse.success(
                newsService.getNews(ticker, page, safeSize, from, to, minImportance)
        ));
    }

    /**
     * GET /api/v1/stocks/{ticker}/summary
     * 종목 LLM 종합 요약 (캐시 6시간).
     */
    @GetMapping("/{ticker}/summary")
    public ResponseEntity<ApiResponse<StockSummaryResponse>> getSummary(
            @PathVariable String ticker
    ) {
        return ResponseEntity.ok(ApiResponse.success(newsService.getSummary(ticker)));
    }
}
