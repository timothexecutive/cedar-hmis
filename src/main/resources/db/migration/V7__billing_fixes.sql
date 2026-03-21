-- V7: Billing hardening fixes

-- Add receipt_no to payments
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS receipt_no VARCHAR(50);

CREATE INDEX IF NOT EXISTS idx_payments_receipt
    ON payments(receipt_no);

-- Cashier sessions table
CREATE SEQUENCE IF NOT EXISTS cashier_sessions_SEQ
    start 1 increment 50;

CREATE TABLE cashier_sessions (
    id             BIGSERIAL PRIMARY KEY,
    cashier        VARCHAR(200) NOT NULL,
    opening_float  NUMERIC(10,2) DEFAULT 0,
    expected_cash  NUMERIC(10,2) DEFAULT 0,
    actual_cash    NUMERIC(10,2) DEFAULT 0,
    variance       NUMERIC(10,2) DEFAULT 0,
    status         VARCHAR(20) DEFAULT 'OPEN',
    opened_at      TIMESTAMPTZ DEFAULT NOW(),
    closed_at      TIMESTAMPTZ,
    notes          TEXT
);

CREATE INDEX idx_sessions_cashier
    ON cashier_sessions(cashier);
CREATE INDEX idx_sessions_status
    ON cashier_sessions(status);

-- Void is a new invoice status — no schema change needed
-- PENDING, PARTIAL, PAID, VOID already handled in application layer