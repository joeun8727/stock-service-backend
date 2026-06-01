package com.stocknews.api.domain.sector;

import com.stocknews.api.client.macro.MacroProvider;
import com.stocknews.api.client.macro.RawMacroData;
import com.stocknews.api.common.config.CacheKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * FRED 거시지표 캐싱 래퍼 (TTL 24시간 — 07-caching.md).
 * SectorRankingService에서 직접 캐싱하면 self-invocation AOP 문제가 발생하므로 별도 빈으로 분리.
 */
@Service
@RequiredArgsConstructor
public class MacroDataService {

    private final MacroProvider macroProvider;

    @Cacheable(value = CacheKeys.MACRO, key = "#seriesId")
    public RawMacroData fetchCached(String seriesId) {
        return macroProvider.fetchSeries(seriesId);
    }
}
