package com.stocknews.api.domain.sector;

import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "sector")
@Getter
@NoArgsConstructor
public class Sector {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "latest_rank")
    private Integer latestRank;

    @Column(name = "latest_score", precision = 5, scale = 2)
    private BigDecimal latestScore;

    @Column(name = "ranked_at")
    private LocalDateTime rankedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public Sector(String code, String name) {
        this.code = code;
        this.name = name;
        this.createdAt = LocalDateTime.now();
    }

    public void updateRanking(int rank, BigDecimal score) {
        this.latestRank = rank;
        this.latestScore = score;
        this.rankedAt = LocalDateTime.now();
    }
}
