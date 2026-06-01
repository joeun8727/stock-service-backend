-- ============================================================
-- StockNews 초기 스키마 (Phase 1 — 미장 전용)
-- 규칙: DECIMAL 사용 (float/double 금지), 원문 본문 저장 금지
-- ============================================================

-- ─────────────────────────────────────────
-- 1. sector
-- ─────────────────────────────────────────
CREATE TABLE sector (
    id           BIGINT          NOT NULL AUTO_INCREMENT,
    code         VARCHAR(50)     NOT NULL COMMENT '섹터 코드 (예: SEMICONDUCTOR)',
    name         VARCHAR(100)    NOT NULL COMMENT '표시명 (예: 반도체)',
    latest_rank  INT             NULL     COMMENT '최신 유망도 순위 (1~5)',
    latest_score DECIMAL(5, 2)   NULL     COMMENT '최신 점수 (0~100)',
    ranked_at    DATETIME        NULL     COMMENT '랭킹 산출 시각',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_sector_code (code)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;


-- ─────────────────────────────────────────
-- 2. stock
-- ─────────────────────────────────────────
CREATE TABLE stock (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    ticker              VARCHAR(20)     NOT NULL COMMENT '티커 (예: AAPL)',
    market              VARCHAR(10)     NOT NULL COMMENT '''US'' / 추후 ''KR''',
    company_name        VARCHAR(255)    NOT NULL,
    sector_id           BIGINT          NULL,
    industry            VARCHAR(100)    NULL,
    market_cap          DECIMAL(20, 2)  NULL     COMMENT '시가총액 (USD)',
    exchange            VARCHAR(50)     NULL     COMMENT '거래소',
    website             VARCHAR(255)    NULL,
    employee_count      INT             NULL,
    ipo_date            DATE            NULL,
    is_growth_candidate BOOLEAN         NOT NULL DEFAULT FALSE COMMENT '성장주 후보 플래그',
    updated_at          DATETIME        NULL,
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_stock_market_ticker (market, ticker),
    CONSTRAINT fk_stock_sector FOREIGN KEY (sector_id) REFERENCES sector (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_stock_sector_id  ON stock (sector_id);
CREATE INDEX idx_stock_market_cap ON stock (market_cap);


-- ─────────────────────────────────────────
-- 3. financial_metric
-- ─────────────────────────────────────────
CREATE TABLE financial_metric (
    id                  BIGINT          NOT NULL AUTO_INCREMENT,
    stock_id            BIGINT          NOT NULL,
    period              VARCHAR(10)     NOT NULL COMMENT '''annual'' / ''quarterly''',
    fiscal_date         DATE            NOT NULL COMMENT '회계 기준일',
    roe                 DECIMAL(10, 4)  NULL     COMMENT '자기자본이익률',
    roa                 DECIMAL(10, 4)  NULL     COMMENT '총자산이익률',
    roic                DECIMAL(10, 4)  NULL     COMMENT '투자자본이익률',
    per                 DECIMAL(10, 4)  NULL     COMMENT '주가수익비율',
    pbr                 DECIMAL(10, 4)  NULL     COMMENT '주가순자산비율',
    eps                 DECIMAL(10, 4)  NULL     COMMENT '주당순이익',
    debt_ratio          DECIMAL(10, 4)  NULL     COMMENT '부채비율',
    interest_coverage   DECIMAL(10, 4)  NULL     COMMENT '이자보상배율',
    revenue_growth_yoy  DECIMAL(10, 4)  NULL     COMMENT '매출성장률 (YoY)',
    operating_margin    DECIMAL(10, 4)  NULL     COMMENT '영업이익률',
    ocf_to_ni           DECIMAL(10, 4)  NULL     COMMENT '영업현금흐름 / 순이익',
    created_at          DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_financial_metric (stock_id, period, fiscal_date),
    CONSTRAINT fk_financial_stock FOREIGN KEY (stock_id) REFERENCES stock (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_financial_stock_fiscal ON financial_metric (stock_id, fiscal_date);


-- ─────────────────────────────────────────
-- 4. news_article
-- 원문 본문(content) 컬럼 절대 저장 금지 (08-compliance.md)
-- ─────────────────────────────────────────
CREATE TABLE news_article (
    id               BIGINT          NOT NULL AUTO_INCREMENT,
    stock_id         BIGINT          NOT NULL,
    source           VARCHAR(100)    NOT NULL COMMENT '출처 (예: Reuters)',
    source_url       VARCHAR(500)    NOT NULL COMMENT '원문 링크 (중복 방지 키)',
    headline         VARCHAR(500)    NOT NULL,
    published_at     DATETIME        NULL,
    summary          TEXT            NULL     COMMENT 'LLM 3줄 요약 (원문 아님)',
    sentiment_score  DECIMAL(3, 2)   NULL     COMMENT '-1.0 ~ 1.0',
    importance_score INT             NULL     COMMENT '0 ~ 100',
    relevance        VARCHAR(10)     NULL     COMMENT 'HIGH / MEDIUM / LOW',
    llm_processed    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_news_source_url (source_url),
    CONSTRAINT fk_news_stock FOREIGN KEY (stock_id) REFERENCES stock (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_news_stock_published ON news_article (stock_id, published_at);


-- ─────────────────────────────────────────
-- 5. api_call_log  (Rate Limit 추적)
-- ─────────────────────────────────────────
CREATE TABLE api_call_log (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    provider  VARCHAR(50)  NOT NULL COMMENT '''finnhub'' / ''fred'' / ''gemini''',
    endpoint  VARCHAR(255) NOT NULL,
    called_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status    VARCHAR(20)  NOT NULL COMMENT 'SUCCESS / RATE_LIMITED / ERROR',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;

CREATE INDEX idx_api_call_log_provider_called ON api_call_log (provider, called_at);


-- ─────────────────────────────────────────
-- 6. 초기 섹터 데이터 (Phase 1 고정 목록)
-- ─────────────────────────────────────────
INSERT INTO sector (code, name) VALUES
    ('SEMICONDUCTOR',    '반도체'),
    ('AEROSPACE',        '우주/항공'),
    ('AI_SOFTWARE',      'AI/소프트웨어'),
    ('EV_BATTERY',       '전기차/배터리'),
    ('HEALTHCARE_BIO',   '헬스케어/바이오'),
    ('ENERGY',           '에너지'),
    ('FINANCE',          '금융'),
    ('CONSUMER_GOODS',   '소비재');
