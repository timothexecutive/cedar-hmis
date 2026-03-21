-- V4: Pharmacy Module

CREATE TABLE drugs (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    generic_name    VARCHAR(200),
    code            VARCHAR(50) UNIQUE NOT NULL,
    category        VARCHAR(100),
    formulation     VARCHAR(100),
    strength        VARCHAR(100),
    unit            VARCHAR(50),
    reorder_level   INT DEFAULT 10,
    current_stock   INT DEFAULT 0,
    buying_price    NUMERIC(10,2) DEFAULT 0,
    selling_price   NUMERIC(10,2) DEFAULT 0,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE prescriptions (
    id              BIGSERIAL PRIMARY KEY,
    prescription_no VARCHAR(20) UNIQUE NOT NULL,
    patient_id      BIGINT NOT NULL REFERENCES patients(id),
    visit_id        BIGINT REFERENCES visits(id),
    prescribed_by   VARCHAR(200),
    status          VARCHAR(20) DEFAULT 'PENDING',
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE prescription_items (
    id              BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id),
    drug_id         BIGINT NOT NULL REFERENCES drugs(id),
    dosage          VARCHAR(200),
    frequency       VARCHAR(100),
    duration        VARCHAR(100),
    quantity        INT NOT NULL,
    instructions    TEXT,
    status          VARCHAR(20) DEFAULT 'PENDING',
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE TABLE dispensing (
    id              BIGSERIAL PRIMARY KEY,
    prescription_id BIGINT NOT NULL REFERENCES prescriptions(id),
    drug_id         BIGINT NOT NULL REFERENCES drugs(id),
    quantity        INT NOT NULL,
    dispensed_by    VARCHAR(200),
    notes           TEXT,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE SEQUENCE IF NOT EXISTS drugs_SEQ              start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS prescriptions_SEQ      start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS prescription_items_SEQ start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS dispensing_SEQ         start 1 increment 50;

CREATE INDEX idx_prescriptions_patient ON prescriptions(patient_id);
CREATE INDEX idx_prescriptions_status  ON prescriptions(status);
CREATE INDEX idx_prescription_items_rx ON prescription_items(prescription_id);
CREATE INDEX idx_dispensing_rx         ON dispensing(prescription_id);
CREATE INDEX idx_drugs_stock           ON drugs(current_stock);

-- Seed common drugs
INSERT INTO drugs (name, generic_name, code, category, formulation, strength, unit, reorder_level, current_stock, buying_price, selling_price) VALUES
('Artemether/Lumefantrine', 'AL', 'AL-80480', 'ANTIMALARIAL', 'TABLET', '80/480mg', 'Tab', 50, 200, 45, 90),
('Amoxicillin', 'Amoxicillin', 'AMOX-500', 'ANTIBIOTIC', 'CAPSULE', '500mg', 'Cap', 100, 500, 8, 15),
('Paracetamol', 'Paracetamol', 'PARA-500', 'ANALGESIC', 'TABLET', '500mg', 'Tab', 200, 1000, 2, 5),
('Ibuprofen', 'Ibuprofen', 'IBU-400', 'ANALGESIC', 'TABLET', '400mg', 'Tab', 100, 400, 5, 10),
('Metronidazole', 'Metronidazole', 'METRO-400', 'ANTIBIOTIC', 'TABLET', '400mg', 'Tab', 100, 300, 4, 8),
('Ciprofloxacin', 'Ciprofloxacin', 'CIPRO-500', 'ANTIBIOTIC', 'TABLET', '500mg', 'Tab', 50, 200, 12, 25),
('Omeprazole', 'Omeprazole', 'OMP-20', 'GASTRO', 'CAPSULE', '20mg', 'Cap', 50, 300, 8, 18),
('Salbutamol Inhaler', 'Salbutamol', 'SALB-INH', 'RESPIRATORY', 'INHALER', '100mcg', 'Puff', 20, 50, 250, 450),
('Atenolol', 'Atenolol', 'ATEN-50', 'CARDIOVASCULAR', 'TABLET', '50mg', 'Tab', 50, 200, 6, 12),
('Metformin', 'Metformin', 'MET-500', 'ANTIDIABETIC', 'TABLET', '500mg', 'Tab', 100, 400, 5, 10),
('Amlodipine', 'Amlodipine', 'AMLO-5', 'CARDIOVASCULAR', 'TABLET', '5mg', 'Tab', 50, 200, 8, 15),
('ORS Sachets', 'ORS', 'ORS-SAC', 'ELECTROLYTE', 'SACHET', '20.5g', 'Sachet', 100, 500, 10, 20),
('IV Normal Saline 500ml', 'Normal Saline', 'NS-500', 'IV FLUID', 'INFUSION', '0.9%', 'Bag', 50, 100, 80, 150),
('IV Dextrose 5% 500ml', 'Dextrose', 'D5-500', 'IV FLUID', 'INFUSION', '5%', 'Bag', 50, 100, 90, 160),
('Quinine IV 600mg', 'Quinine', 'QUI-IV', 'ANTIMALARIAL', 'INFUSION', '600mg', 'Vial', 20, 50, 200, 380),
('Gentamicin 80mg', 'Gentamicin', 'GENT-80', 'ANTIBIOTIC', 'INJECTION', '80mg', 'Vial', 20, 80, 50, 100),
('Diclofenac 75mg', 'Diclofenac', 'DICLO-75', 'ANALGESIC', 'INJECTION', '75mg', 'Amp', 30, 100, 30, 60),
('Folic Acid', 'Folic Acid', 'FOLIC-5', 'SUPPLEMENT', 'TABLET', '5mg', 'Tab', 100, 500, 1, 3),
('Ferrous Sulphate', 'Ferrous Sulphate', 'FER-SUL', 'SUPPLEMENT', 'TABLET', '200mg', 'Tab', 100, 500, 2, 5),
('Cotrimoxazole', 'Cotrimoxazole', 'COTRI-480', 'ANTIBIOTIC', 'TABLET', '480mg', 'Tab', 100, 400, 3, 6);