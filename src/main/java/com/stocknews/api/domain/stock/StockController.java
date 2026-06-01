package com.stocknews.api.domain.stock;

import com.stocknews.api.common.response.ApiResponse;
import com.stocknews.api.domain.stock.dto.StockProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

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
}
