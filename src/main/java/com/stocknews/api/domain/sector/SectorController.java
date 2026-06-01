package com.stocknews.api.domain.sector;

import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.common.response.ApiResponse;
import com.stocknews.api.domain.sector.dto.SectorRankingResponse;
import com.stocknews.api.domain.sector.dto.SectorStocksResponse;
import com.stocknews.api.domain.sector.dto.SectorTrendResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sectors")
@RequiredArgsConstructor
public class SectorController {

    private final SectorRankingService sectorRankingService;
    private final StockScreeningService stockScreeningService;

    /**
     * GET /api/v1/sectors/ranking?limit=5
     * 유망 섹터 상위 N위 (기본 5, 최대 10). 캐시 12시간, 배치 산출.
     */
    @GetMapping("/ranking")
    public ResponseEntity<ApiResponse<SectorRankingResponse>> getRanking(
            @RequestParam(defaultValue = "5") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 10);
        return ResponseEntity.ok(ApiResponse.success(sectorRankingService.getRankings(safeLimit)));
    }

    /**
     * GET /api/v1/sectors/{sectorId}/trend
     * 섹터 트렌드 상세 — 최근 14일 일별 뉴스 수 + 평균 감정.
     */
    @GetMapping("/{sectorId}/trend")
    public ResponseEntity<ApiResponse<SectorTrendResponse>> getSectorTrend(
            @PathVariable Long sectorId
    ) {
        return ResponseEntity.ok(ApiResponse.success(sectorRankingService.getSectorTrend(sectorId)));
    }

    /**
     * GET /api/v1/sectors/{sectorId}/stocks?type=large_cap|growth&limit=20
     * 섹터별 종목 Top N 스크리닝.
     */
    @GetMapping("/{sectorId}/stocks")
    public ResponseEntity<ApiResponse<SectorStocksResponse>> getSectorStocks(
            @PathVariable Long sectorId,
            @RequestParam String type,
            @RequestParam(defaultValue = "20") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);

        SectorStocksResponse response = switch (type) {
            case "large_cap" -> stockScreeningService.getLargeCapStocks(sectorId, safeLimit);
            case "growth"    -> stockScreeningService.getGrowthStocks(sectorId, safeLimit);
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT,
                    "type은 'large_cap' 또는 'growth'여야 합니다: " + type);
        };

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
