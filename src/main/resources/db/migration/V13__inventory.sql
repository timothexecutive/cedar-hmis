-- V12: Inventory Module
-- Covers: Suppliers, Items, Batches, Purchase Orders,
--         Goods Received, Issuances, Adjustments

-- ── Suppliers ─────────────────────────────────────────────────────────────
CREATE TABLE suppliers (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    code            VARCHAR(50) UNIQUE NOT NULL,
    contact_person  VARCHAR(200),
    phone           VARCHAR(20),
    email           VARCHAR(200),
    address         TEXT,
    kra_pin         VARCHAR(50),
    supplier_type   VARCHAR(30) DEFAULT 'PRIVATE',
    credit_days     INTEGER DEFAULT 30,
    credit_limit    DECIMAL(15,2) DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── Inventory Items ───────────────────────────────────────────────────────
CREATE TABLE inventory_items (
    id                  BIGSERIAL PRIMARY KEY,
    name                VARCHAR(200) NOT NULL,
    code                VARCHAR(50) UNIQUE NOT NULL,
    category            VARCHAR(50) NOT NULL,
    sub_category        VARCHAR(50),
    unit_of_measure     VARCHAR(30) NOT NULL,
    description         TEXT,
    reorder_level       INTEGER DEFAULT 10,
    reorder_quantity    INTEGER DEFAULT 50,
    storage_condition   VARCHAR(100) DEFAULT 'ROOM_TEMP',
    is_controlled       BOOLEAN DEFAULT FALSE,
    requires_cold_chain BOOLEAN DEFAULT FALSE,
    is_drug             BOOLEAN DEFAULT FALSE,
    drug_id             BIGINT REFERENCES drugs(id),
    is_active           BOOLEAN DEFAULT TRUE,
    created_by          VARCHAR(200),
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Inventory Batches ─────────────────────────────────────────────────────
CREATE TABLE inventory_batches (
    id                  BIGSERIAL PRIMARY KEY,
    item_id             BIGINT NOT NULL REFERENCES inventory_items(id),
    batch_no            VARCHAR(100) NOT NULL,
    supplier_id         BIGINT REFERENCES suppliers(id),
    manufacture_date    DATE,
    expiry_date         DATE,
    quantity_received   INTEGER NOT NULL,
    quantity_remaining  INTEGER NOT NULL,
    unit_cost           DECIMAL(15,2),
    selling_price       DECIMAL(15,2),
    storage_location    VARCHAR(100),
    received_date       DATE DEFAULT CURRENT_DATE,
    received_by         VARCHAR(200),
    is_expired          BOOLEAN DEFAULT FALSE,
    is_active           BOOLEAN DEFAULT TRUE,
    notes               TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Purchase Orders ───────────────────────────────────────────────────────
CREATE TABLE purchase_orders (
    id              BIGSERIAL PRIMARY KEY,
    lpo_no          VARCHAR(50) UNIQUE NOT NULL,
    supplier_id     BIGINT NOT NULL REFERENCES suppliers(id),
    order_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    expected_date   DATE,
    total_amount    DECIMAL(15,2) DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'DRAFT',
    approved_by     VARCHAR(200),
    approved_at     TIMESTAMPTZ,
    rejection_reason TEXT,
    delivery_address TEXT,
    payment_terms   VARCHAR(100),
    raised_by       VARCHAR(200),
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── Purchase Order Items ──────────────────────────────────────────────────
CREATE TABLE purchase_order_items (
    id              BIGSERIAL PRIMARY KEY,
    po_id           BIGINT NOT NULL REFERENCES purchase_orders(id),
    item_id         BIGINT NOT NULL REFERENCES inventory_items(id),
    quantity        INTEGER NOT NULL,
    unit_cost       DECIMAL(15,2) NOT NULL,
    total           DECIMAL(15,2) NOT NULL,
    quantity_received INTEGER DEFAULT 0,
    status          VARCHAR(20) DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── Goods Received Notes ──────────────────────────────────────────────────
CREATE TABLE goods_received (
    id              BIGSERIAL PRIMARY KEY,
    grn_no          VARCHAR(50) UNIQUE NOT NULL,
    po_id           BIGINT REFERENCES purchase_orders(id),
    supplier_id     BIGINT NOT NULL REFERENCES suppliers(id),
    received_date   DATE NOT NULL DEFAULT CURRENT_DATE,
    invoice_no      VARCHAR(100),
    invoice_date    DATE,
    invoice_amount  DECIMAL(15,2),
    status          VARCHAR(20) DEFAULT 'RECEIVED',
    received_by     VARCHAR(200),
    verified_by     VARCHAR(200),
    verified_at     TIMESTAMPTZ,
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── Goods Received Items ──────────────────────────────────────────────────
CREATE TABLE goods_received_items (
    id              BIGSERIAL PRIMARY KEY,
    grn_id          BIGINT NOT NULL REFERENCES goods_received(id),
    item_id         BIGINT NOT NULL REFERENCES inventory_items(id),
    batch_id        BIGINT REFERENCES inventory_batches(id),
    quantity        INTEGER NOT NULL,
    unit_cost       DECIMAL(15,2),
    total           DECIMAL(15,2),
    batch_no        VARCHAR(100),
    expiry_date     DATE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── Stock Issuances ───────────────────────────────────────────────────────
CREATE TABLE stock_issuances (
    id              BIGSERIAL PRIMARY KEY,
    issuance_no     VARCHAR(50) UNIQUE NOT NULL,
    department      VARCHAR(100) NOT NULL,
    requested_by    VARCHAR(200),
    issued_by       VARCHAR(200),
    patient_id      BIGINT REFERENCES patients(id),
    purpose         TEXT,
    status          VARCHAR(20) DEFAULT 'ISSUED',
    issued_date     DATE NOT NULL DEFAULT CURRENT_DATE,
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── Stock Issuance Items ──────────────────────────────────────────────────
CREATE TABLE stock_issuance_items (
    id              BIGSERIAL PRIMARY KEY,
    issuance_id     BIGINT NOT NULL REFERENCES stock_issuances(id),
    item_id         BIGINT NOT NULL REFERENCES inventory_items(id),
    batch_id        BIGINT REFERENCES inventory_batches(id),
    quantity        INTEGER NOT NULL,
    unit_cost       DECIMAL(15,2),
    total           DECIMAL(15,2),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── Stock Adjustments ─────────────────────────────────────────────────────
CREATE TABLE stock_adjustments (
    id              BIGSERIAL PRIMARY KEY,
    adjustment_no   VARCHAR(50) UNIQUE NOT NULL,
    item_id         BIGINT NOT NULL REFERENCES inventory_items(id),
    batch_id        BIGINT REFERENCES inventory_batches(id),
    adjustment_type VARCHAR(30) NOT NULL,
    quantity_before INTEGER NOT NULL,
    quantity_change INTEGER NOT NULL,
    quantity_after  INTEGER NOT NULL,
    reason          TEXT NOT NULL,
    approved_by     VARCHAR(200),
    adjusted_by     VARCHAR(200),
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── Sequences ─────────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS suppliers_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS inventory_items_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS inventory_batches_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS purchase_orders_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS purchase_order_items_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS goods_received_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS goods_received_items_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS stock_issuances_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS stock_issuance_items_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS stock_adjustments_SEQ
    START WITH 1 INCREMENT BY 50;

-- ── Indexes ───────────────────────────────────────────────────────────────
CREATE INDEX idx_inv_items_category
    ON inventory_items(category);
CREATE INDEX idx_inv_items_active
    ON inventory_items(is_active);
CREATE INDEX idx_inv_batches_item
    ON inventory_batches(item_id);
CREATE INDEX idx_inv_batches_expiry
    ON inventory_batches(expiry_date);
CREATE INDEX idx_inv_batches_active
    ON inventory_batches(is_active);
CREATE INDEX idx_po_supplier
    ON purchase_orders(supplier_id);
CREATE INDEX idx_po_status
    ON purchase_orders(status);
CREATE INDEX idx_grn_supplier
    ON goods_received(supplier_id);
CREATE INDEX idx_grn_po
    ON goods_received(po_id);
CREATE INDEX idx_issuance_dept
    ON stock_issuances(department);
CREATE INDEX idx_issuance_patient
    ON stock_issuances(patient_id);
CREATE INDEX idx_adjustment_item
    ON stock_adjustments(item_id);

-- ── Seed Suppliers ────────────────────────────────────────────────────────
INSERT INTO suppliers
    (name, code, contact_person, phone,
     email, supplier_type, credit_days) VALUES
('Medisel Kenya Ltd',
    'SUP-001', 'Sales Team',
    '0800720601',
    'orders@medisel.co.ke', 'PRIVATE', 30),
('Biomed East Africa',
    'SUP-002', 'Sales Team',
    '0722000000',
    'sales@biomed.co.ke', 'PRIVATE', 30),
('KEMSA',
    'SUP-003', 'KEMSA Supplies',
    '0800720601',
    'orders@kemsa.go.ke', 'GOVERNMENT', 0),
('Philips Medical Kenya',
    'SUP-004', 'Service Team',
    '0733000000',
    'service@philips.co.ke', 'PRIVATE', 45);

-- ── Seed Core Inventory Items ─────────────────────────────────────────────
INSERT INTO inventory_items
    (name, code, category, unit_of_measure,
     reorder_level, reorder_quantity,
     storage_condition) VALUES
('Examination Gloves (Medium)',
    'INV-001', 'SUPPLIES', 'Box',
    10, 50, 'ROOM_TEMP'),
('Examination Gloves (Large)',
    'INV-002', 'SUPPLIES', 'Box',
    10, 50, 'ROOM_TEMP'),
('Surgical Gloves Size 7.5',
    'INV-003', 'SUPPLIES', 'Pair',
    20, 100, 'ROOM_TEMP'),
('IV Cannula 18G',
    'INV-004', 'SUPPLIES', 'Piece',
    50, 200, 'ROOM_TEMP'),
('IV Cannula 20G',
    'INV-005', 'SUPPLIES', 'Piece',
    50, 200, 'ROOM_TEMP'),
('IV Infusion Set',
    'INV-006', 'SUPPLIES', 'Piece',
    30, 100, 'ROOM_TEMP'),
('Normal Saline 500ml',
    'INV-007', 'FLUIDS', 'Bag',
    30, 100, 'ROOM_TEMP'),
('Dextrose 5% 500ml',
    'INV-008', 'FLUIDS', 'Bag',
    20, 50, 'ROOM_TEMP'),
('Ringer''s Lactate 500ml',
    'INV-009', 'FLUIDS', 'Bag',
    20, 50, 'ROOM_TEMP'),
('Surgical Suture Vicryl 1',
    'INV-010', 'THEATRE', 'Piece',
    20, 50, 'ROOM_TEMP'),
('Surgical Drape Set',
    'INV-011', 'THEATRE', 'Set',
    10, 30, 'ROOM_TEMP'),
('Surgical Gown',
    'INV-012', 'THEATRE', 'Piece',
    15, 50, 'ROOM_TEMP'),
('Gauze Swab 10x10',
    'INV-013', 'SUPPLIES', 'Pack',
    20, 100, 'ROOM_TEMP'),
('Crepe Bandage 10cm',
    'INV-014', 'SUPPLIES', 'Roll',
    20, 100, 'ROOM_TEMP'),
('Urinary Catheter 14FR',
    'INV-015', 'SUPPLIES', 'Piece',
    10, 30, 'ROOM_TEMP'),
('Oxygen Cylinder (Large)',
    'INV-016', 'EQUIPMENT', 'Cylinder',
    5, 10, 'ROOM_TEMP'),
('Bed Sheet',
    'INV-017', 'LINEN', 'Piece',
    20, 50, 'ROOM_TEMP'),
('Pillow Case',
    'INV-018', 'LINEN', 'Piece',
    20, 50, 'ROOM_TEMP'),
('Syringe 5ml',
    'INV-019', 'SUPPLIES', 'Piece',
    100, 500, 'ROOM_TEMP'),
('Syringe 10ml',
    'INV-020', 'SUPPLIES', 'Piece',
    100, 500, 'ROOM_TEMP'),
('Blood Collection Tube EDTA',
    'INV-021', 'LAB', 'Piece',
    50, 200, 'ROOM_TEMP'),
('Urine Collection Cup',
    'INV-022', 'LAB', 'Piece',
    30, 100, 'ROOM_TEMP'),
('Alcohol Hand Rub 500ml',
    'INV-023', 'HOUSEKEEPING', 'Bottle',
    10, 30, 'ROOM_TEMP'),
('Chlorine Disinfectant 5L',
    'INV-024', 'HOUSEKEEPING', 'Jerry Can',
    5, 20, 'ROOM_TEMP'),
('MOH Antenatal Register',
    'INV-025', 'STATIONERY', 'Book',
    5, 10, 'ROOM_TEMP');