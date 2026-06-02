package com.stocknews.api.domain.sector;

import com.stocknews.api.common.exception.BusinessException;
import com.stocknews.api.common.exception.ErrorCode;
import com.stocknews.api.common.response.ApiResponse;
import com.stocknews.api.domain.sector.dto.RuleOf40Response;
import com.stocknews.api.domain.sector.dto.SectorRankingResponse;
import com.stocknews.api.domain.sector.dto.SectorStocksResponse;
import com.stocknews.api.domain.sector.dto.SectorTrendResponse;
import com.stocknews.api.domain.sector.dto.ValuationResponse;
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

    /**
     * GET /api/v1/sectors/{sectorId}/rule-of-40?limit=20
     * Rule of 40 리더보드 — 매출성장률% + FCF마진%(없으면 영업이익률%) (캐시 6시간).
     * GROWTH_TECH 섹터에서 특히 유의미.
     */
    @GetMapping("/{sectorId}/rule-of-40")
    public ResponseEntity<ApiResponse<RuleOf40Response>> getRuleOf40(
            @PathVariable Long sectorId,
            @RequestParam(defaultValue = "20") int limit
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        return ResponseEntity.ok(ApiResponse.success(sectorRankingService.getRuleOf40(sectorId, safeLimit)));
    }

    /**
     * GET /api/v1/sectors/{sectorId}/valuation
     * 섹터 내 종목 밸류에이션 비교 — PER/PBR/PSR/PEG 일괄 조회, 시총 내림차순 (캐시 6시간).
     */
    @GetMapping("/{sectorId}/valuation")
    public ResponseEntity<ApiResponse<ValuationResponse>> getSectorValuation(
            @PathVariable Long sectorId
    ) {
        return ResponseEntity.ok(ApiResponse.success(sectorRankingService.getSectorValuation(sectorId)));
    }
}
