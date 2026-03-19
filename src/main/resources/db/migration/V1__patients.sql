-- Cedar Hospital HMIS
-- V1: Patient table
CREATE SEQUENCE IF NOT EXISTS patients_SEQ start 1 increment 50;
CREATE TABLE patients (
    id            BIGSERIAL PRIMARY KEY,
    patient_no    VARCHAR(20)  UNIQUE NOT NULL,
    full_name     VARCHAR(200) NOT NULL,
    national_id   VARCHAR(20)  UNIQUE,
    phone         VARCHAR(20)  NOT NULL,
    gender        VARCHAR(10)  NOT NULL,
    date_of_birth DATE,
    county        VARCHAR(100),
    next_of_kin_name  VARCHAR(200),
    next_of_kin_phone VARCHAR(20),
    sha_member_no     VARCHAR(50),
    is_sha_member     BOOLEAN DEFAULT FALSE,
    is_active         BOOLEAN DEFAULT TRUE,
    created_at    TIMESTAMPTZ DEFAULT NOW(),
    updated_at    TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_patients_national_id ON patients(national_id);
CREATE INDEX idx_patients_phone       ON patients(phone);
CREATE INDEX idx_patients_patient_no  ON patients(patient_no);