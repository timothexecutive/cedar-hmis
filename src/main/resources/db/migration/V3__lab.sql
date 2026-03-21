-- V3: Laboratory Module

CREATE TABLE lab_tests (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    code        VARCHAR(50)  UNIQUE NOT NULL,
    category    VARCHAR(100),
    price       NUMERIC(10,2) DEFAULT 0,
    turnaround  INT DEFAULT 1,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE lab_requests (
    id              BIGSERIAL PRIMARY KEY,
    request_no      VARCHAR(20) UNIQUE NOT NULL,
    patient_id      BIGINT NOT NULL REFERENCES patients(id),
    visit_id        BIGINT REFERENCES visits(id),
    requested_by    VARCHAR(200),
    status          VARCHAR(20) DEFAULT 'PENDING',
    priority        VARCHAR(20) DEFAULT 'ROUTINE',
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE lab_results (
    id              BIGSERIAL PRIMARY KEY,
    request_id      BIGINT NOT NULL REFERENCES lab_requests(id),
    test_id         BIGINT NOT NULL REFERENCES lab_tests(id),
    result_value    TEXT,
    unit            VARCHAR(50),
    reference_range VARCHAR(100),
    flag            VARCHAR(20) DEFAULT 'NORMAL',
    notes           TEXT,
    done_by         VARCHAR(200),
    verified_by     VARCHAR(200),
    status          VARCHAR(20) DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE SEQUENCE IF NOT EXISTS lab_tests_SEQ    start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS lab_requests_SEQ start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS lab_results_SEQ  start 1 increment 50;

CREATE INDEX idx_lab_requests_patient ON lab_requests(patient_id);
CREATE INDEX idx_lab_requests_status  ON lab_requests(status);
CREATE INDEX idx_lab_results_request  ON lab_results(request_id);

-- Seed common lab tests
INSERT INTO lab_tests (name, code, category, price, turnaround) VALUES
('Full Blood Count',           'FBC',      'HAEMATOLOGY',  500,  4),
('Malaria RDT',                'MAL-RDT',  'PARASITOLOGY', 300,  1),
('Malaria Smear',              'MAL-SMR',  'PARASITOLOGY', 400,  2),
('Blood Sugar (RBS)',          'RBS',      'CHEMISTRY',    200,  1),
('Blood Sugar (FBS)',          'FBS',      'CHEMISTRY',    200,  1),
('Urine Analysis',             'U/A',      'URINALYSIS',   300,  2),
('Urine Culture',              'U/C',      'MICROBIOLOGY', 800,  48),
('Widal Test',                 'WIDAL',    'SEROLOGY',     500,  4),
('HIV Test',                   'HIV',      'SEROLOGY',     300,  1),
('Hepatitis B Surface Antigen','HBsAg',    'SEROLOGY',     500,  2),
('Pregnancy Test (urine)',     'BHCG-U',   'SEROLOGY',     300,  1),
('Liver Function Tests',       'LFTs',     'CHEMISTRY',    1200, 6),
('Renal Function Tests',       'RFTs',     'CHEMISTRY',    1200, 6),
('Lipid Profile',              'LIPIDS',   'CHEMISTRY',    1500, 6),
('Thyroid Function (TSH)',     'TSH',      'CHEMISTRY',    1500, 6),
('CD4 Count',                  'CD4',      'IMMUNOLOGY',   2000, 24),
('Sputum AFB',                 'AFB',      'MICROBIOLOGY', 500,  24),
('Stool Analysis',             'STOOL',    'PARASITOLOGY', 400,  4),
('Blood Culture',              'B/C',      'MICROBIOLOGY', 1500, 72),
('Electrolytes',               'ELECTRO',  'CHEMISTRY',    1000, 4);