-- ============================================================
-- V4: 스코어링 테이블 추가 + FinancialMetric 컬럼 보강
-- ============================================================

-- ─────────────────────────────────────────
-- 1. financial_metric — 신규 스냅샷 컬럼 추가
-- ─────────────────────────────────────────
ALTER TABLE financial_metric
    ADD COLUMN psr         DECIMAL(10,4) NULL COMMENT 'Price/Sales TTM (psTTM)',
    ADD COLUMN peg         DECIMAL(10,4) NULL COMMENT 'PEG ratio TTM (음수/null = 적자)',
    ADD COLUMN gross_margin DECIMAL(10,4) NULL COMMENT 'Gross Margin TTM',
    ADD COLUMN fcf_margin  DECIMAL(10,4) NULL COMMENT 'FCF Margin (시계열 최신값)';

-- ─────────────────────────────────────────
-- 2. metric_trend — 추세 기울기 캐싱
-- ─────────────────────────────────────────
CREATE TABLE metric_trend (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    stock_id       BIGINT        NOT NULL,
    metric_name    VARCHAR(50)   NOT NULL COMMENT 'grossMargin / roic / operatingMargin / fcfMargin',
    slope          DECIMAL(12,6) NOT NULL COMMENT '선형회귀 기울기',
    data_points    INT           NOT NULL COMMENT '유효 분기 수',
    calculated_at  TIMESTAMP     NOT NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_metric_trend (stock_id, metric_name),
    CONSTRAINT fk_metric_trend_stock FOREIGN KEY (stock_id) REFERENCES stock (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

-- ─────────────────────────────────────────
-- 3. stock_score — 스크리닝 결과 저장
-- ─────────────────────────────────────────
CREATE TABLE stock_score (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    stock_id        BIGINT        NOT NULL,
    sector_id       BIGINT        NOT NULL,
    screen_type     VARCHAR(20)   NOT NULL COMMENT 'LARGE_CAP / GROWTH',
    sector_group    VARCHAR(20)   NOT NULL COMMENT 'GROWTH_TECH / TRADITIONAL',
    total_score     DECIMAL(5,2)  NOT NULL COMMENT '0~100 최종점수',
    rank_in_sector  INT           NOT NULL COMMENT '섹터 내 순위',
    score_detail    JSON          NULL     COMMENT '지표별 백분위 내역',
    scored_at       TIMESTAMP     NOT NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NULL ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_score (stock_id, screen_type),
    CONSTRAINT fk_stock_score_stock  FOREIGN KEY (stock_id)  REFERENCES stock  (id),
    CONSTRAINT fk_stock_score_sector FOREIGN KEY (sector_id) REFERENCES sector (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_stock_score_sector_type_rank ON stock_score (sector_id, screen_type, rank_in_sector);
