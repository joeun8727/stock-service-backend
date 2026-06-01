-- ============================================================
-- 신규 섹터 추가: 로봇/자동화, 사이버보안
-- ============================================================

INSERT INTO sector (code, name) VALUES
    ('ROBOTICS',      '로봇/자동화'),
    ('CYBERSECURITY', '사이버보안');

-- ─────────────────────────────────────────
-- 로봇/자동화 (ROBOTICS)
-- 산업용 로봇, 의료로봇, 자율주행, 물류자동화
-- ─────────────────────────────────────────
INSERT INTO stock (ticker, market, company_name, sector_id, industry, market_cap, exchange) VALUES
('ISRG', 'US', 'Intuitive Surgical Inc.',      (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'Medical Robotics',        175000000000, 'NASDAQ'),
('ABB',  'US', 'ABB Ltd',                       (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'Industrial Automation',    90000000000, 'NYSE'),
('EMR',  'US', 'Emerson Electric Co.',          (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'Industrial Automation',    60000000000, 'NYSE'),
('ROK',  'US', 'Rockwell Automation Inc.',      (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'Industrial Automation',    30000000000, 'NYSE'),
('AXON', 'US', 'Axon Enterprise Inc.',          (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'Connected Devices & AI',   28000000000, 'NASDAQ'),
('SYM',  'US', 'Symbotic Inc.',                 (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'Warehouse Robotics',       20000000000, 'NASDAQ'),
('TER',  'US', 'Teradyne Inc.',                 (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'Collaborative Robots',     18000000000, 'NASDAQ'),
('ZBRA', 'US', 'Zebra Technologies Corporation',(SELECT id FROM sector WHERE code = 'ROBOTICS'), 'Enterprise Automation',    16000000000, 'NASDAQ'),
('CGNX', 'US', 'Cognex Corporation',            (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'Machine Vision',           12000000000, 'NASDAQ'),
('MBLY', 'US', 'Mobileye Global Inc.',          (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'Autonomous Driving AI',    12000000000, 'NASDAQ'),
('JOBY', 'US', 'Joby Aviation Inc.',            (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'eVTOL / Air Mobility',      6000000000, 'NYSE'),
('ACHR', 'US', 'Archer Aviation Inc.',          (SELECT id FROM sector WHERE code = 'ROBOTICS'), 'eVTOL / Air Mobility',      4000000000, 'NYSE');

-- ─────────────────────────────────────────
-- 사이버보안 (CYBERSECURITY)
-- 네트워크 보안, 엔드포인트, 클라우드 보안, ID 관리
-- ─────────────────────────────────────────
INSERT INTO stock (ticker, market, company_name, sector_id, industry, market_cap, exchange) VALUES
('PANW', 'US', 'Palo Alto Networks Inc.',       (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Network Security',        120000000000, 'NASDAQ'),
('CRWD', 'US', 'CrowdStrike Holdings Inc.',     (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Endpoint Security',       100000000000, 'NASDAQ'),
('FTNT', 'US', 'Fortinet Inc.',                 (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Network Security',         70000000000, 'NASDAQ'),
('NET',  'US', 'Cloudflare Inc.',               (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Cloud Security & CDN',     40000000000, 'NYSE'),
('ZS',   'US', 'Zscaler Inc.',                  (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Cloud Security',           35000000000, 'NASDAQ'),
('OKTA', 'US', 'Okta Inc.',                     (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Identity & Access Mgmt',   20000000000, 'NASDAQ'),
('S',    'US', 'SentinelOne Inc.',              (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Endpoint Security',        20000000000, 'NYSE'),
('CYBR', 'US', 'CyberArk Software Ltd.',        (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Privileged Access Mgmt',   15000000000, 'NASDAQ'),
('VRNS', 'US', 'Varonis Systems Inc.',          (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Data Security',             7000000000, 'NASDAQ'),
('TENB', 'US', 'Tenable Holdings Inc.',         (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Vulnerability Management',  6000000000, 'NASDAQ'),
('QLYS', 'US', 'Qualys Inc.',                   (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Cloud Security',            6000000000, 'NASDAQ'),
('RPD',  'US', 'Rapid7 Inc.',                   (SELECT id FROM sector WHERE code = 'CYBERSECURITY'), 'Security Analytics',        3000000000, 'NASDAQ');
