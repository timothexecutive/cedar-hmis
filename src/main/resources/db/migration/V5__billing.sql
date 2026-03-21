-- V5: Billing Module + Barcodes

-- Add barcode to existing tables
ALTER TABLE patients ADD COLUMN IF NOT EXISTS barcode VARCHAR(50) UNIQUE;
ALTER TABLE drugs    ADD COLUMN IF NOT EXISTS barcode VARCHAR(50) UNIQUE;
ALTER TABLE lab_requests ADD COLUMN IF NOT EXISTS sample_barcode VARCHAR(50) UNIQUE;

-- Update existing patients with auto-generated barcodes
UPDATE patients SET barcode = 'PAT-' || LPAD(id::TEXT, 8, '0') WHERE barcode IS NULL;
UPDATE drugs    SET barcode = 'DRG-' || LPAD(id::TEXT, 8, '0') WHERE barcode IS NULL;

-- ── INSURANCE PROVIDERS ───────────────────────────────
CREATE TABLE insurance_providers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(50)  UNIQUE NOT NULL,
    provider_type   VARCHAR(50)  DEFAULT 'PRIVATE',
    contact_person  VARCHAR(200),
    contact_phone   VARCHAR(20),
    contact_email   VARCHAR(200),
    credit_limit    NUMERIC(10,2) DEFAULT 0,
    payment_terms   INT DEFAULT 30,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── INVOICES ──────────────────────────────────────────
CREATE TABLE invoices (
    id              BIGSERIAL PRIMARY KEY,
    invoice_no      VARCHAR(20)  UNIQUE NOT NULL,
    patient_id      BIGINT NOT NULL REFERENCES patients(id),
    visit_id        BIGINT REFERENCES visits(id),
    admission_id    BIGINT REFERENCES admissions(id),
    invoice_type    VARCHAR(20)  DEFAULT 'OPD',
    status          VARCHAR(20)  DEFAULT 'PENDING',
    subtotal        NUMERIC(10,2) DEFAULT 0,
    discount        NUMERIC(10,2) DEFAULT 0,
    tax             NUMERIC(10,2) DEFAULT 0,
    total_amount    NUMERIC(10,2) DEFAULT 0,
    insurance_amount NUMERIC(10,2) DEFAULT 0,
    patient_amount  NUMERIC(10,2) DEFAULT 0,
    paid_amount     NUMERIC(10,2) DEFAULT 0,
    balance         NUMERIC(10,2) DEFAULT 0,
    notes           TEXT,
    created_by      VARCHAR(200),
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── INVOICE ITEMS ─────────────────────────────────────
CREATE TABLE invoice_items (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      BIGINT NOT NULL REFERENCES invoices(id),
    description     VARCHAR(300) NOT NULL,
    category        VARCHAR(50),
    quantity        INT DEFAULT 1,
    unit_price      NUMERIC(10,2) NOT NULL,
    total           NUMERIC(10,2) NOT NULL,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── PAYMENTS ──────────────────────────────────────────
CREATE TABLE payments (
    id              BIGSERIAL PRIMARY KEY,
    invoice_id      BIGINT NOT NULL REFERENCES invoices(id),
    payment_method  VARCHAR(20)  NOT NULL,
    amount          NUMERIC(10,2) NOT NULL,
    reference_no    VARCHAR(100),
    mpesa_receipt   VARCHAR(50),
    phone_number    VARCHAR(20),
    verified        BOOLEAN DEFAULT FALSE,
    cashier         VARCHAR(200),
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── INSURANCE CLAIMS ──────────────────────────────────
CREATE TABLE insurance_claims (
    id                  BIGSERIAL PRIMARY KEY,
    claim_no            VARCHAR(20)  UNIQUE NOT NULL,
    invoice_id          BIGINT NOT NULL REFERENCES invoices(id),
    provider_id         BIGINT NOT NULL REFERENCES insurance_providers(id),
    patient_id          BIGINT NOT NULL REFERENCES patients(id),
    member_no           VARCHAR(100),
    amount_claimed      NUMERIC(10,2) DEFAULT 0,
    amount_approved     NUMERIC(10,2) DEFAULT 0,
    amount_paid         NUMERIC(10,2) DEFAULT 0,
    status              VARCHAR(20)  DEFAULT 'SUBMITTED',
    claim_ref_no        VARCHAR(100),
    submission_date     TIMESTAMPTZ DEFAULT NOW(),
    approval_date       TIMESTAMPTZ,
    payment_date        TIMESTAMPTZ,
    rejection_reason    TEXT,
    notes               TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── MPESA CALLBACKS ───────────────────────────────────
CREATE TABLE mpesa_callbacks (
    id                  BIGSERIAL PRIMARY KEY,
    checkout_request_id VARCHAR(100),
    merchant_request_id VARCHAR(100),
    result_code         INT,
    result_desc         TEXT,
    mpesa_receipt       VARCHAR(50),
    amount              NUMERIC(10,2),
    phone_number        VARCHAR(20),
    raw_payload         TEXT,
    processed           BOOLEAN DEFAULT FALSE,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

CREATE SEQUENCE IF NOT EXISTS insurance_providers_SEQ start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS invoices_SEQ            start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS invoice_items_SEQ       start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS payments_SEQ            start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS insurance_claims_SEQ    start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS mpesa_callbacks_SEQ     start 1 increment 50;

CREATE INDEX idx_invoices_patient    ON invoices(patient_id);
CREATE INDEX idx_invoices_status     ON invoices(status);
CREATE INDEX idx_invoice_items       ON invoice_items(invoice_id);
CREATE INDEX idx_payments_invoice    ON payments(invoice_id);
CREATE INDEX idx_claims_invoice      ON insurance_claims(invoice_id);
CREATE INDEX idx_claims_provider     ON insurance_claims(provider_id);
CREATE INDEX idx_mpesa_checkout      ON mpesa_callbacks(checkout_request_id);

-- ── SEED INSURANCE PROVIDERS ──────────────────────────
INSERT INTO insurance_providers (name, code, provider_type, payment_terms) VALUES
('Social Health Authority',    'SHA',      'GOVERNMENT', 30),
('AAR Insurance',              'AAR',      'PRIVATE',    30),
('Jubilee Health Insurance',   'JUBILEE',  'PRIVATE',    30),
('Britam Health',              'BRITAM',   'PRIVATE',    30),
('CIC Insurance',              'CIC',      'PRIVATE',    30),
('Madison Insurance',          'MADISON',  'PRIVATE',    30),
('UAP Insurance',              'UAP',      'PRIVATE',    30),
('Resolution Insurance',       'RESOLTN',  'PRIVATE',    30),
('Sanlam Insurance',           'SANLAM',   'PRIVATE',    30),
('NHIF Corporate Scheme',      'NHIF-CRP', 'GOVERNMENT', 30),
('UNHCR',                      'UNHCR',    'AID',        15),
('Kenya Red Cross',            'KRC',      'AID',        15);