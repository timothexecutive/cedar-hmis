-- ══════════════════════════════════════════════════════
-- Cedar Hospital HMIS — Complete Database Schema
-- ══════════════════════════════════════════════════════

-- ── SEQUENCES ─────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS patients_SEQ     start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS visits_SEQ       start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS triage_SEQ       start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS wards_SEQ        start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS beds_SEQ         start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS admissions_SEQ   start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS ward_rounds_SEQ  start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS users_SEQ        start 1 increment 50;
CREATE SEQUENCE IF NOT EXISTS audit_logs_SEQ   start 1 increment 50;

-- ── PATIENTS ──────────────────────────────────────────
CREATE TABLE patients (
    id                BIGSERIAL PRIMARY KEY,
    patient_no        VARCHAR(20)  UNIQUE NOT NULL,
    full_name         VARCHAR(200) NOT NULL,
    national_id       VARCHAR(20)  UNIQUE,
    phone             VARCHAR(20)  NOT NULL,
    gender            VARCHAR(10)  NOT NULL,
    date_of_birth     DATE,
    county            VARCHAR(100),
    next_of_kin_name  VARCHAR(200),
    next_of_kin_phone VARCHAR(20),
    sha_member_no     VARCHAR(50),
    is_sha_member     BOOLEAN DEFAULT FALSE,
    is_active         BOOLEAN DEFAULT TRUE,
    created_at        TIMESTAMPTZ DEFAULT NOW(),
    updated_at        TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_patients_national_id ON patients(national_id);
CREATE INDEX idx_patients_phone       ON patients(phone);
CREATE INDEX idx_patients_patient_no  ON patients(patient_no);

-- ── VISITS (OPD) ──────────────────────────────────────
CREATE TABLE visits (
    id               BIGSERIAL PRIMARY KEY,
    visit_no         VARCHAR(20) UNIQUE NOT NULL,
    patient_id       BIGINT NOT NULL REFERENCES patients(id),
    visit_date       DATE NOT NULL DEFAULT CURRENT_DATE,
    visit_type       VARCHAR(20) DEFAULT 'OPD',
    status           VARCHAR(20) DEFAULT 'WAITING',
    queue_number     INT,
    assigned_doctor  VARCHAR(200),
    chief_complaint  TEXT,
    diagnosis        TEXT,
    notes            TEXT,
    created_at       TIMESTAMPTZ DEFAULT NOW(),
    updated_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_visits_patient ON visits(patient_id);
CREATE INDEX idx_visits_status  ON visits(status);
CREATE INDEX idx_visits_date    ON visits(visit_date);

-- ── TRIAGE ────────────────────────────────────────────
CREATE TABLE triage (
    id               BIGSERIAL PRIMARY KEY,
    visit_id         BIGINT NOT NULL REFERENCES visits(id),
    blood_pressure   VARCHAR(20),
    temperature      NUMERIC(4,1),
    pulse            INT,
    weight           NUMERIC(5,1),
    height           NUMERIC(5,1),
    spo2             INT,
    rbs              NUMERIC(5,1),
    triage_category  VARCHAR(20) DEFAULT 'NON_URGENT',
    notes            TEXT,
    done_by          VARCHAR(200),
    created_at       TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_triage_visit ON triage(visit_id);

-- ── WARDS ─────────────────────────────────────────────
CREATE TABLE wards (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    code        VARCHAR(20)  UNIQUE NOT NULL,
    ward_type   VARCHAR(50),
    total_beds  INT DEFAULT 0,
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

-- ── BEDS ──────────────────────────────────────────────
CREATE TABLE beds (
    id          BIGSERIAL PRIMARY KEY,
    bed_number  VARCHAR(20) NOT NULL,
    ward_id     BIGINT NOT NULL REFERENCES wards(id),
    status      VARCHAR(20) DEFAULT 'AVAILABLE',
    bed_type    VARCHAR(50) DEFAULT 'GENERAL',
    is_active   BOOLEAN DEFAULT TRUE,
    created_at  TIMESTAMPTZ DEFAULT NOW(),
    updated_at  TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(bed_number, ward_id)
);

CREATE INDEX idx_beds_ward   ON beds(ward_id);
CREATE INDEX idx_beds_status ON beds(status);

-- ── ADMISSIONS ────────────────────────────────────────
CREATE TABLE admissions (
    id                   BIGSERIAL PRIMARY KEY,
    admission_no         VARCHAR(20) UNIQUE NOT NULL,
    patient_id           BIGINT NOT NULL REFERENCES patients(id),
    bed_id               BIGINT NOT NULL REFERENCES beds(id),
    ward_id              BIGINT NOT NULL REFERENCES wards(id),
    admitting_doctor     VARCHAR(200),
    admission_diagnosis  TEXT,
    admission_date       TIMESTAMPTZ DEFAULT NOW(),
    discharge_date       TIMESTAMPTZ,
    status               VARCHAR(20) DEFAULT 'ADMITTED',
    discharge_summary    TEXT,
    created_at           TIMESTAMPTZ DEFAULT NOW(),
    updated_at           TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_admissions_patient ON admissions(patient_id);
CREATE INDEX idx_admissions_status  ON admissions(status);

-- ── WARD ROUNDS ───────────────────────────────────────
CREATE TABLE ward_rounds (
    id            BIGSERIAL PRIMARY KEY,
    admission_id  BIGINT NOT NULL REFERENCES admissions(id),
    doctor        VARCHAR(200),
    notes         TEXT,
    plan          TEXT,
    round_date    TIMESTAMPTZ DEFAULT NOW()
);

-- ── USERS ─────────────────────────────────────────────
CREATE TABLE users (
    id              BIGSERIAL PRIMARY KEY,
    staff_no        VARCHAR(20)  UNIQUE NOT NULL,
    full_name       VARCHAR(200) NOT NULL,
    email           VARCHAR(200) UNIQUE,
    phone           VARCHAR(20)  NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    role            VARCHAR(50)  NOT NULL,
    department      VARCHAR(100),
    is_active       BOOLEAN DEFAULT TRUE,
    failed_attempts INT DEFAULT 0,
    locked_until    TIMESTAMPTZ,
    last_login      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ DEFAULT NOW(),
    updated_at      TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_users_email    ON users(email);
CREATE INDEX idx_users_staff_no ON users(staff_no);

-- ── AUDIT LOGS ────────────────────────────────────────
CREATE TABLE audit_logs (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT REFERENCES users(id),
    user_name   VARCHAR(200),
    action      VARCHAR(200) NOT NULL,
    resource    VARCHAR(200),
    resource_id VARCHAR(100),
    details     TEXT,
    ip_address  VARCHAR(50),
    created_at  TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_audit_user    ON audit_logs(user_id);
CREATE INDEX idx_audit_action  ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);

-- ══════════════════════════════════════════════════════
-- SEED DATA
-- ══════════════════════════════════════════════════════

-- ── WARDS ─────────────────────────────────────────────
INSERT INTO wards (name, code, ward_type, total_beds) VALUES
('Male Medical Ward',   'MMW', 'MEDICAL',    16),
('Female Medical Ward', 'FMW', 'MEDICAL',    12),
('Paediatric Ward',     'PAW', 'PAEDIATRIC', 10),
('Maternity Ward',      'MAW', 'MATERNITY',   8),
('Surgical Ward',       'SRW', 'SURGICAL',   10),
('Private Ward',        'PRW', 'PRIVATE',     6);

-- ── BEDS — Male Medical Ward (ward 1) ─────────────────
INSERT INTO beds (bed_number, ward_id) VALUES
('M-01',1),('M-02',1),('M-03',1),('M-04',1),
('M-05',1),('M-06',1),('M-07',1),('M-08',1),
('M-09',1),('M-10',1),('M-11',1),('M-12',1),
('M-13',1),('M-14',1),('M-15',1),('M-16',1);

-- ── BEDS — Female Medical Ward (ward 2) ───────────────
INSERT INTO beds (bed_number, ward_id) VALUES
('F-01',2),('F-02',2),('F-03',2),('F-04',2),
('F-05',2),('F-06',2),('F-07',2),('F-08',2),
('F-09',2),('F-10',2),('F-11',2),('F-12',2);

-- ── BEDS — Paediatric Ward (ward 3) ───────────────────
INSERT INTO beds (bed_number, ward_id) VALUES
('P-01',3),('P-02',3),('P-03',3),('P-04',3),
('P-05',3),('P-06',3),('P-07',3),('P-08',3),
('P-09',3),('P-10',3);

-- ── BEDS — Maternity Ward (ward 4) ────────────────────
INSERT INTO beds (bed_number, ward_id) VALUES
('LW-01',4),('LW-02',4),('PN-01',4),('PN-02',4),
('PN-03',4),('PN-04',4),('PN-05',4),('PN-06',4);

-- ── BEDS — Surgical Ward (ward 5) ─────────────────────
INSERT INTO beds (bed_number, ward_id) VALUES
('S-01',5),('S-02',5),('S-03',5),('S-04',5),
('S-05',5),('S-06',5),('S-07',5),('S-08',5),
('S-09',5),('S-10',5);

-- ── BEDS — Private Ward (ward 6) ──────────────────────
INSERT INTO beds (bed_number, ward_id) VALUES
('PR-01',6),('PR-02',6),('PR-03',6),
('PR-04',6),('PR-05',6),('PR-06',6);

-- ── DEFAULT ADMIN USER ────────────────────────────────
-- Password: Admin@Cedar2026
INSERT INTO users (
    staff_no,
    full_name,
    email,
    phone,
    password_hash,
    role,
    department
) VALUES (
    'ADMIN-001',
    'System Administrator',
    'admin@cedarhospital.co.ke',
    '0700000000',
    '$2a$12$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi.',
    'ADMIN',
    'Administration'
);