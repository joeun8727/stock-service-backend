package com.stocknews.api.scheduler;

import com.stocknews.api.common.config.CacheKeys;
import com.stocknews.api.domain.sector.SectorRankingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 섹터 랭킹 스케줄러 — 매일 06:00 KST (04-sector-ranking.md).
 * 점수 산출 후 캐시 무효화로 다음 요청 시 최신 랭킹 반영.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SectorRankingScheduler {

    private final SectorRankingService sectorRankingService;

    @Scheduled(cron = "${scheduler.sector-ranking.cron}")
    @CacheEvict(value = CacheKeys.SECTOR_RANKING, allEntries = true)
    public void calculateRanking() {
        log.info("섹터 랭킹 산출 스케줄러 시작");
        try {
            sectorRankingService.calculateAndSaveRankings();
        } catch (Exception e) {
            log.error("섹터 랭킹 산출 실패", e);
        }
        log.info("섹터 랭킹 산출 스케줄러 완료");
    }
}
