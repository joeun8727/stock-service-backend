package com.stocknews.api.domain.financial;

import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.common.response.ApiResponse;
import com.stocknews.api.domain.financial.dto.FinancialMetricResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/api/v1/stocks")
@RequiredArgsConstructor
public class FinancialController {

    private static final Set<String> VALID_PERIODS = Set.of("annual", "quarterly");

    private final FinancialService financialService;

    /**
     * GET /api/v1/stocks/{ticker}/financials?period=annual|quarterly&limit=4
     * 종목 재무지표 시계열 (캐시 1시간).
     */
    @GetMapping("/{ticker}/financials")
    public ResponseEntity<ApiResponse<FinancialMetricResponse>> getFinancials(
            @PathVariable String ticker,
            @RequestParam(defaultValue = "quarterly") String period,
            @RequestParam(defaultValue = "4") int limit
    ) {
        if (!VALID_PERIODS.contains(period)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "period는 'annual' 또는 'quarterly'여야 합니다: " + period);
        }
        int safeLimit = Math.min(Math.max(limit, 1), 20);
        return ResponseEntity.ok(ApiResponse.success(
                financialService.getFinancials(ticker, period, safeLimit)));
    }
}
