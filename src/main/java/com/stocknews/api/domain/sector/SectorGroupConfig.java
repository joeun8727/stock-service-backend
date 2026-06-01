package com.stocknews.api.domain.sector;

import java.util.Map;
import java.util.Set;

/**
 * 섹터 코드 → 스코어링 그룹 매핑.
 * 매핑 없는 섹터는 TRADITIONAL(기본값) 처리 + 경고 로그.
 */
public final class SectorGroupConfig {

    private SectorGroupConfig() {}

    public enum SectorGroup { GROWTH_TECH, TRADITIONAL }

    private static final Map<String, SectorGroup> MAP = Map.of(
            "AI_SOFTWARE",   SectorGroup.GROWTH_TECH,
            "ROBOTICS",      SectorGroup.GROWTH_TECH,
            "CYBERSECURITY", SectorGroup.GROWTH_TECH,
            "EV_BATTERY",    SectorGroup.GROWTH_TECH,
            "AEROSPACE",     SectorGroup.GROWTH_TECH,
            "SEMICONDUCTOR", SectorGroup.TRADITIONAL,
            "ENERGY",        SectorGroup.TRADITIONAL,
            "FINANCE",       SectorGroup.TRADITIONAL,
            "CONSUMER_GOODS",SectorGroup.TRADITIONAL,
            "HEALTHCARE_BIO",SectorGroup.TRADITIONAL
    );

    public static SectorGroup of(String sectorCode) {
        return MAP.getOrDefault(sectorCode, SectorGroup.TRADITIONAL);
    }

    public static Set<String> growthTechCodes() {
        return Set.of("AI_SOFTWARE", "ROBOTICS", "CYBERSECURITY", "EV_BATTERY", "AEROSPACE");
    }
}
