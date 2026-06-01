-- ============================================================
-- Phase 1 초기 종목 시드 데이터 (미장 US 전용)
-- market_cap: USD 단위 (FinancialMetricsScheduler 실행 시 자동 갱신)
-- sector_id: 서브쿼리로 code 기반 참조 (순서 의존 제거)
-- ============================================================

-- ─────────────────────────────────────────
-- 1. 반도체 (SEMICONDUCTOR)
--    대형주 + 성장 후보 혼합
-- ─────────────────────────────────────────
INSERT INTO stock (ticker, market, company_name, sector_id, industry, market_cap, exchange) VALUES
('NVDA',  'US', 'NVIDIA Corporation',              (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 2900000000000, 'NASDAQ'),
('AVGO',  'US', 'Broadcom Inc.',                   (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 700000000000,  'NASDAQ'),
('TSM',   'US', 'Taiwan Semiconductor Mfg Co.',    (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 600000000000,  'NYSE'),
('ASML',  'US', 'ASML Holding N.V.',               (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductor Equipment', 310000000000, 'NASDAQ'),
('AMD',   'US', 'Advanced Micro Devices Inc.',     (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 250000000000,  'NASDAQ'),
('QCOM',  'US', 'QUALCOMM Incorporated',           (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 180000000000,  'NASDAQ'),
('AMAT',  'US', 'Applied Materials Inc.',          (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductor Equipment', 140000000000, 'NASDAQ'),
('TXN',   'US', 'Texas Instruments Incorporated',  (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 150000000000,  'NASDAQ'),
('MU',    'US', 'Micron Technology Inc.',          (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 90000000000,   'NASDAQ'),
('KLAC',  'US', 'KLA Corporation',                 (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductor Equipment', 85000000000,  'NASDAQ'),
('LRCX',  'US', 'Lam Research Corporation',        (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductor Equipment', 80000000000,  'NASDAQ'),
('INTC',  'US', 'Intel Corporation',               (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 100000000000,  'NASDAQ'),
('MRVL',  'US', 'Marvell Technology Inc.',         (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 70000000000,   'NASDAQ'),
('NXPI',  'US', 'NXP Semiconductors N.V.',         (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 55000000000,   'NASDAQ'),
('ON',    'US', 'ON Semiconductor Corporation',    (SELECT id FROM sector WHERE code = 'SEMICONDUCTOR'), 'Semiconductors', 25000000000,   'NASDAQ');

-- ─────────────────────────────────────────
-- 2. 우주/항공 (AEROSPACE)
-- ─────────────────────────────────────────
INSERT INTO stock (ticker, market, company_name, sector_id, industry, market_cap, exchange) VALUES
('GE',    'US', 'GE Aerospace',                    (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 220000000000, 'NYSE'),
('RTX',   'US', 'RTX Corporation',                 (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 160000000000, 'NYSE'),
('LMT',   'US', 'Lockheed Martin Corporation',     (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 115000000000, 'NYSE'),
('BA',    'US', 'The Boeing Company',              (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 110000000000, 'NYSE'),
('NOC',   'US', 'Northrop Grumman Corporation',    (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 65000000000,  'NYSE'),
('GD',    'US', 'General Dynamics Corporation',    (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 80000000000,  'NYSE'),
('TDG',   'US', 'TransDigm Group Incorporated',    (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 70000000000,  'NYSE'),
('LDOS',  'US', 'Leidos Holdings Inc.',            (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 22000000000,  'NYSE'),
('KTOS',  'US', 'Kratos Defense & Security',       (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 5000000000,   'NASDAQ'),
('RKLB',  'US', 'Rocket Lab USA Inc.',             (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 9000000000,   'NASDAQ'),
('ASTS',  'US', 'AST SpaceMobile Inc.',            (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 4000000000,   'NASDAQ'),
('HEI',   'US', 'HEICO Corporation',               (SELECT id FROM sector WHERE code = 'AEROSPACE'), 'Aerospace & Defense', 22000000000,  'NYSE');

-- ─────────────────────────────────────────
-- 3. AI/소프트웨어 (AI_SOFTWARE)
-- ─────────────────────────────────────────
INSERT INTO stock (ticker, market, company_name, sector_id, industry, market_cap, exchange) VALUES
('MSFT',  'US', 'Microsoft Corporation',           (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 3000000000000, 'NASDAQ'),
('GOOGL', 'US', 'Alphabet Inc.',                   (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Internet Services', 2100000000000, 'NASDAQ'),
('META',  'US', 'Meta Platforms Inc.',             (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Internet Services', 1400000000000, 'NASDAQ'),
('CRM',   'US', 'Salesforce Inc.',                 (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 280000000000,  'NYSE'),
('NOW',   'US', 'ServiceNow Inc.',                 (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 210000000000,  'NYSE'),
('PLTR',  'US', 'Palantir Technologies Inc.',      (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 220000000000,  'NYSE'),
('SNOW',  'US', 'Snowflake Inc.',                  (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 50000000000,   'NYSE'),
('DDOG',  'US', 'Datadog Inc.',                    (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 45000000000,   'NASDAQ'),
('MDB',   'US', 'MongoDB Inc.',                    (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 22000000000,   'NASDAQ'),
('AI',    'US', 'C3.ai Inc.',                      (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 4000000000,    'NYSE'),
('PATH',  'US', 'UiPath Inc.',                     (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 10000000000,   'NYSE'),
('SOUN',  'US', 'SoundHound AI Inc.',              (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 3000000000,    'NASDAQ'),
('BBAI',  'US', 'BigBear.ai Holdings Inc.',        (SELECT id FROM sector WHERE code = 'AI_SOFTWARE'), 'Software', 1000000000,    'NYSE');

-- ─────────────────────────────────────────
-- 4. 전기차/배터리 (EV_BATTERY)
-- ─────────────────────────────────────────
INSERT INTO stock (ticker, market, company_name, sector_id, industry, market_cap, exchange) VALUES
('TSLA',  'US', 'Tesla Inc.',                      (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'Electric Vehicles', 900000000000,  'NASDAQ'),
('LI',    'US', 'Li Auto Inc.',                    (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'Electric Vehicles', 25000000000,   'NASDAQ'),
('NIO',   'US', 'NIO Inc.',                        (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'Electric Vehicles', 12000000000,   'NYSE'),
('XPEV',  'US', 'XPeng Inc.',                      (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'Electric Vehicles', 16000000000,   'NYSE'),
('RIVN',  'US', 'Rivian Automotive Inc.',          (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'Electric Vehicles', 16000000000,   'NASDAQ'),
('LCID',  'US', 'Lucid Group Inc.',                (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'Electric Vehicles', 8000000000,    'NASDAQ'),
('ALB',   'US', 'Albemarle Corporation',           (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'Lithium & Battery Materials', 7000000000,  'NYSE'),
('SQM',   'US', 'Sociedad Quimica y Minera',       (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'Lithium & Battery Materials', 9000000000,  'NYSE'),
('QS',    'US', 'QuantumScape Corporation',        (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'Battery Technology', 2500000000,   'NYSE'),
('CHPT',  'US', 'ChargePoint Holdings Inc.',       (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'EV Charging', 1200000000,    'NYSE'),
('LAC',   'US', 'Lithium Americas Corp.',          (SELECT id FROM sector WHERE code = 'EV_BATTERY'), 'Lithium & Battery Materials', 1000000000,  'NYSE');

-- ─────────────────────────────────────────
-- 5. 헬스케어/바이오 (HEALTHCARE_BIO)
-- ─────────────────────────────────────────
INSERT INTO stock (ticker, market, company_name, sector_id, industry, market_cap, exchange) VALUES
('LLY',   'US', 'Eli Lilly and Company',           (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Pharmaceuticals', 800000000000,  'NYSE'),
('JNJ',   'US', 'Johnson & Johnson',               (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Pharmaceuticals', 400000000000,  'NYSE'),
('ABBV',  'US', 'AbbVie Inc.',                     (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Pharmaceuticals', 370000000000,  'NYSE'),
('MRK',   'US', 'Merck & Co. Inc.',               (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Pharmaceuticals', 260000000000,  'NYSE'),
('VRTX',  'US', 'Vertex Pharmaceuticals Inc.',     (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Biotechnology', 130000000000,  'NASDAQ'),
('AMGN',  'US', 'Amgen Inc.',                      (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Biotechnology', 155000000000,  'NASDAQ'),
('GILD',  'US', 'Gilead Sciences Inc.',            (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Biotechnology', 105000000000,  'NASDAQ'),
('REGN',  'US', 'Regeneron Pharmaceuticals',       (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Biotechnology', 85000000000,   'NASDAQ'),
('MRNA',  'US', 'Moderna Inc.',                    (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Biotechnology', 15000000000,   'NASDAQ'),
('BNTX',  'US', 'BioNTech SE',                     (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Biotechnology', 30000000000,   'NASDAQ'),
('CRSP',  'US', 'CRISPR Therapeutics AG',          (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Biotechnology', 4000000000,    'NASDAQ'),
('BEAM',  'US', 'Beam Therapeutics Inc.',          (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Biotechnology', 1500000000,    'NASDAQ'),
('NTLA',  'US', 'Intellia Therapeutics Inc.',      (SELECT id FROM sector WHERE code = 'HEALTHCARE_BIO'), 'Biotechnology', 2000000000,    'NASDAQ');

-- ─────────────────────────────────────────
-- 6. 에너지 (ENERGY)
-- ─────────────────────────────────────────
INSERT INTO stock (ticker, market, company_name, sector_id, industry, market_cap, exchange) VALUES
('XOM',   'US', 'Exxon Mobil Corporation',         (SELECT id FROM sector WHERE code = 'ENERGY'), 'Oil & Gas', 490000000000,  'NYSE'),
('CVX',   'US', 'Chevron Corporation',             (SELECT id FROM sector WHERE code = 'ENERGY'), 'Oil & Gas', 280000000000,  'NYSE'),
('NEE',   'US', 'NextEra Energy Inc.',             (SELECT id FROM sector WHERE code = 'ENERGY'), 'Renewable Energy', 145000000000, 'NYSE'),
('COP',   'US', 'ConocoPhillips',                  (SELECT id FROM sector WHERE code = 'ENERGY'), 'Oil & Gas', 130000000000,  'NYSE'),
('SLB',   'US', 'SLB (Schlumberger)',              (SELECT id FROM sector WHERE code = 'ENERGY'), 'Oil Field Services', 60000000000, 'NYSE'),
('OXY',   'US', 'Occidental Petroleum Corporation',(SELECT id FROM sector WHERE code = 'ENERGY'), 'Oil & Gas', 50000000000,   'NYSE'),
('MPC',   'US', 'Marathon Petroleum Corporation',  (SELECT id FROM sector WHERE code = 'ENERGY'), 'Oil Refining', 55000000000,  'NYSE'),
('VLO',   'US', 'Valero Energy Corporation',       (SELECT id FROM sector WHERE code = 'ENERGY'), 'Oil Refining', 45000000000,  'NYSE'),
('FSLR',  'US', 'First Solar Inc.',                (SELECT id FROM sector WHERE code = 'ENERGY'), 'Solar Energy', 20000000000,  'NASDAQ'),
('ENPH',  'US', 'Enphase Energy Inc.',             (SELECT id FROM sector WHERE code = 'ENERGY'), 'Solar Energy', 10000000000,  'NASDAQ'),
('RUN',   'US', 'Sunrun Inc.',                     (SELECT id FROM sector WHERE code = 'ENERGY'), 'Solar Energy', 3000000000,   'NASDAQ'),
('EOG',   'US', 'EOG Resources Inc.',              (SELECT id FROM sector WHERE code = 'ENERGY'), 'Oil & Gas', 70000000000,   'NYSE');

-- ─────────────────────────────────────────
-- 7. 금융 (FINANCE)
-- ─────────────────────────────────────────
INSERT INTO stock (ticker, market, company_name, sector_id, industry, market_cap, exchange) VALUES
('BRK.B', 'US', 'Berkshire Hathaway Inc.',         (SELECT id FROM sector WHERE code = 'FINANCE'), 'Financial Conglomerate', 1000000000000, 'NYSE'),
('V',     'US', 'Visa Inc.',                       (SELECT id FROM sector WHERE code = 'FINANCE'), 'Payment Processing', 650000000000,  'NYSE'),
('JPM',   'US', 'JPMorgan Chase & Co.',            (SELECT id FROM sector WHERE code = 'FINANCE'), 'Banking', 700000000000,  'NYSE'),
('MA',    'US', 'Mastercard Incorporated',         (SELECT id FROM sector WHERE code = 'FINANCE'), 'Payment Processing', 510000000000,  'NYSE'),
('BAC',   'US', 'Bank of America Corporation',     (SELECT id FROM sector WHERE code = 'FINANCE'), 'Banking', 340000000000,  'NYSE'),
('WFC',   'US', 'Wells Fargo & Company',           (SELECT id FROM sector WHERE code = 'FINANCE'), 'Banking', 225000000000,  'NYSE'),
('GS',    'US', 'The Goldman Sachs Group Inc.',    (SELECT id FROM sector WHERE code = 'FINANCE'), 'Investment Banking', 190000000000, 'NYSE'),
('AXP',   'US', 'American Express Company',        (SELECT id FROM sector WHERE code = 'FINANCE'), 'Payment Processing', 210000000000,  'NYSE'),
('MS',    'US', 'Morgan Stanley',                  (SELECT id FROM sector WHERE code = 'FINANCE'), 'Investment Banking', 185000000000,  'NYSE'),
('BLK',   'US', 'BlackRock Inc.',                  (SELECT id FROM sector WHERE code = 'FINANCE'), 'Asset Management', 150000000000,  'NYSE'),
('COIN',  'US', 'Coinbase Global Inc.',            (SELECT id FROM sector WHERE code = 'FINANCE'), 'Cryptocurrency', 65000000000,   'NASDAQ'),
('SOFI',  'US', 'SoFi Technologies Inc.',          (SELECT id FROM sector WHERE code = 'FINANCE'), 'Fintech', 12000000000,   'NASDAQ'),
('HOOD',  'US', 'Robinhood Markets Inc.',          (SELECT id FROM sector WHERE code = 'FINANCE'), 'Fintech', 18000000000,   'NASDAQ');

-- ─────────────────────────────────────────
-- 8. 소비재 (CONSUMER_GOODS)
-- ─────────────────────────────────────────
INSERT INTO stock (ticker, market, company_name, sector_id, industry, market_cap, exchange) VALUES
('AMZN',  'US', 'Amazon.com Inc.',                 (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'E-Commerce', 2100000000000, 'NASDAQ'),
('WMT',   'US', 'Walmart Inc.',                    (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Retail', 750000000000,  'NYSE'),
('COST',  'US', 'Costco Wholesale Corporation',    (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Retail', 420000000000,  'NASDAQ'),
('HD',    'US', 'The Home Depot Inc.',             (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Home Improvement Retail', 390000000000, 'NYSE'),
('PG',    'US', 'The Procter & Gamble Company',   (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Consumer Products', 385000000000,  'NYSE'),
('MCD',   'US', 'McDonald''s Corporation',         (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Restaurants', 215000000000,  'NYSE'),
('KO',    'US', 'The Coca-Cola Company',           (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Beverages', 290000000000,  'NYSE'),
('PEP',   'US', 'PepsiCo Inc.',                   (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Beverages', 200000000000,  'NASDAQ'),
('NKE',   'US', 'NIKE Inc.',                       (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Apparel & Footwear', 95000000000,  'NYSE'),
('SBUX',  'US', 'Starbucks Corporation',           (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Restaurants', 90000000000,   'NASDAQ'),
('TGT',   'US', 'Target Corporation',              (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Retail', 65000000000,   'NYSE'),
('LULU',  'US', 'Lululemon Athletica Inc.',        (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'Apparel & Footwear', 42000000000,  'NASDAQ'),
('ETSY',  'US', 'Etsy Inc.',                       (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'E-Commerce', 8000000000,    'NASDAQ'),
('W',     'US', 'Wayfair Inc.',                    (SELECT id FROM sector WHERE code = 'CONSUMER_GOODS'), 'E-Commerce', 4500000000,    'NYSE');
