package com.stocknews.api.domain.sector;

import com.stocknews.api.support.RepositoryTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SectorRepositoryTest extends RepositoryTestSupport {

    @Autowired
    SectorRepository sectorRepository;

    @Autowired
    EntityManager em;

    @Test
    void 저장_후_기본정보_조회() {
        Sector sector = Sector.builder().code("SEMICONDUCTOR").name("반도체").build();

        Sector saved = sectorRepository.saveAndFlush(sector);
        em.clear();

        Sector found = sectorRepository.findById(saved.getId()).orElseThrow();
        assertThat(found.getCode()).isEqualTo("SEMICONDUCTOR");
        assertThat(found.getName()).isEqualTo("반도체");
        assertThat(found.getCreatedAt()).isNotNull();
        assertThat(found.getLatestRank()).isNull();
        assertThat(found.getLatestScore()).isNull();
    }

    @Test
    void code_유니크_제약_중복_저장시_예외() {
        sectorRepository.saveAndFlush(Sector.builder().code("SEMICONDUCTOR").name("반도체").build());

        assertThatThrownBy(() ->
                sectorRepository.saveAndFlush(Sector.builder().code("SEMICONDUCTOR").name("중복").build())
        ).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void findByCode_존재하는_섹터() {
        sectorRepository.saveAndFlush(Sector.builder().code("AI_SOFTWARE").name("AI/소프트웨어").build());

        Optional<Sector> result = sectorRepository.findByCode("AI_SOFTWARE");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("AI/소프트웨어");
    }

    @Test
    void findByCode_없는_섹터() {
        Optional<Sector> result = sectorRepository.findByCode("NONEXISTENT");

        assertThat(result).isEmpty();
    }

    @Test
    void findAllByOrderByLatestRankAsc_랭킹순_정렬() {
        Sector s1 = sectorRepository.saveAndFlush(Sector.builder().code("ENERGY").name("에너지").build());
        Sector s2 = sectorRepository.saveAndFlush(Sector.builder().code("FINANCE").name("금융").build());
        Sector s3 = sectorRepository.saveAndFlush(Sector.builder().code("HEALTHCARE_BIO").name("헬스케어").build());

        s3.updateRanking(1, BigDecimal.valueOf(90.0));
        s1.updateRanking(2, BigDecimal.valueOf(80.0));
        s2.updateRanking(3, BigDecimal.valueOf(70.0));
        sectorRepository.flush();
        em.clear();

        List<Sector> ranked = sectorRepository.findAllByOrderByLatestRankAsc();
        // V1__init.sql의 초기 8개 섹터도 포함되어 있으나 rank=null은 후순위로 배치됨
        List<Sector> withRank = ranked.stream().filter(s -> s.getLatestRank() != null).toList();
        assertThat(withRank).extracting(Sector::getLatestRank).containsExactly(1, 2, 3);
    }

    @Test
    void updateRanking_랭킹_갱신() {
        Sector sector = sectorRepository.saveAndFlush(Sector.builder().code("EV_BATTERY").name("전기차/배터리").build());

        sector.updateRanking(1, BigDecimal.valueOf(87.5));
        sectorRepository.flush();
        em.clear();

        Sector updated = sectorRepository.findById(sector.getId()).orElseThrow();
        assertThat(updated.getLatestRank()).isEqualTo(1);
        assertThat(updated.getLatestScore()).isEqualByComparingTo(BigDecimal.valueOf(87.5));
        assertThat(updated.getRankedAt()).isNotNull();
    }
}
