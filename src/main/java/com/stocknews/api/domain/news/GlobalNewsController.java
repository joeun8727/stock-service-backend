package com.stocknews.api.domain.news;

import com.stocknews.api.common.response.ApiResponse;
import com.stocknews.api.domain.news.dto.TopNewsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
public class GlobalNewsController {

    private final NewsService newsService;

    /**
     * GET /api/v1/news/top?minImportance=70&size=30
     * 전체 종목 횡단 중요 뉴스 피드 — importance 필터 후 최신순 (캐시 30분).
     */
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<TopNewsResponse>> getTopNews(
            @RequestParam(required = false) Integer minImportance,
            @RequestParam(defaultValue = "30") int size
    ) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        return ResponseEntity.ok(ApiResponse.success(newsService.getTopNews(minImportance, safeSize)));
    }
}
