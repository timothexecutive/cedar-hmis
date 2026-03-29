-- V11: Theatre Module
-- Covers: Theatre Rooms, Surgery Bookings, Pre-Op, Intra-Op, Post-Op

-- ── Theatre Rooms ─────────────────────────────────────────────────────────
CREATE TABLE theatre_rooms (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    code            VARCHAR(20) UNIQUE NOT NULL,
    room_type       VARCHAR(30) DEFAULT 'GENERAL',
    status          VARCHAR(20) DEFAULT 'AVAILABLE',
    floor           VARCHAR(20),
    notes           TEXT,
    is_active       BOOLEAN DEFAULT TRUE,
    created_at      TIMESTAMPTZ DEFAULT NOW()
);

-- ── Surgery Bookings ──────────────────────────────────────────────────────
CREATE TABLE surgery_bookings (
    id                  BIGSERIAL PRIMARY KEY,
    booking_no          VARCHAR(50) UNIQUE NOT NULL,
    patient_id          BIGINT NOT NULL REFERENCES patients(id),
    theatre_room_id     BIGINT NOT NULL REFERENCES theatre_rooms(id),
    ipd_admission_id    BIGINT REFERENCES admissions(id),
    maternity_id        BIGINT REFERENCES maternity_admissions(id),
    surgery_type        VARCHAR(100) NOT NULL,
    surgery_category    VARCHAR(30) DEFAULT 'ELECTIVE',
    diagnosis           TEXT,
    planned_date        DATE NOT NULL,
    planned_start_time  TIME,
    planned_duration    INTEGER,
    lead_surgeon        VARCHAR(200),
    assistant_surgeon   VARCHAR(200),
    anaesthetist        VARCHAR(200),
    scrub_nurse         VARCHAR(200),
    circulating_nurse   VARCHAR(200),
    anaesthesia_type    VARCHAR(50),
    special_equipment   TEXT,
    blood_ordered       BOOLEAN DEFAULT FALSE,
    blood_units         INTEGER DEFAULT 0,
    consent_signed      BOOLEAN DEFAULT FALSE,
    status              VARCHAR(20) DEFAULT 'SCHEDULED',
    cancellation_reason TEXT,
    booked_by           VARCHAR(200),
    notes               TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Pre-Op Checklist (WHO Sign In) ────────────────────────────────────────
CREATE TABLE preop_checklists (
    id                      BIGSERIAL PRIMARY KEY,
    booking_id              BIGINT NOT NULL REFERENCES surgery_bookings(id),
    patient_id              BIGINT NOT NULL REFERENCES patients(id),
    -- Patient verification
    identity_confirmed      BOOLEAN DEFAULT FALSE,
    consent_confirmed       BOOLEAN DEFAULT FALSE,
    site_marked             BOOLEAN DEFAULT FALSE,
    -- Clinical checks
    anaesthesia_checked     BOOLEAN DEFAULT FALSE,
    pulse_oximeter_working  BOOLEAN DEFAULT FALSE,
    allergies_checked       BOOLEAN DEFAULT FALSE,
    difficult_airway        BOOLEAN DEFAULT FALSE,
    aspiration_risk         BOOLEAN DEFAULT FALSE,
    blood_loss_risk         VARCHAR(20) DEFAULT 'LOW',
    -- Vitals before surgery
    bp_systolic             INTEGER,
    bp_diastolic            INTEGER,
    pulse                   INTEGER,
    temperature             DECIMAL(4,1),
    spo2                    INTEGER,
    weight                  DECIMAL(5,2),
    -- Preparation
    fasting_confirmed       BOOLEAN DEFAULT FALSE,
    fasting_hours           INTEGER,
    iv_access               BOOLEAN DEFAULT FALSE,
    premedication_given     BOOLEAN DEFAULT FALSE,
    premedication_details   VARCHAR(200),
    jewellery_removed       BOOLEAN DEFAULT FALSE,
    nail_polish_removed     BOOLEAN DEFAULT FALSE,
    -- Completion
    completed_by            VARCHAR(200),
    completed_at            TIMESTAMPTZ,
    notes                   TEXT,
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

-- ── Intraoperative Record (WHO Time Out + Sign Out) ───────────────────────
CREATE TABLE intraop_records (
    id                      BIGSERIAL PRIMARY KEY,
    booking_id              BIGINT NOT NULL REFERENCES surgery_bookings(id),
    patient_id              BIGINT NOT NULL REFERENCES patients(id),
    -- WHO Time Out
    timeout_done            BOOLEAN DEFAULT FALSE,
    team_introduced         BOOLEAN DEFAULT FALSE,
    site_confirmed          BOOLEAN DEFAULT FALSE,
    antibiotic_given        BOOLEAN DEFAULT FALSE,
    antibiotic_name         VARCHAR(100),
    -- Surgery timing
    actual_start_time       TIME,
    actual_end_time         TIME,
    duration_minutes        INTEGER,
    -- Anaesthesia
    anaesthesia_type        VARCHAR(50),
    anaesthesia_start       TIME,
    anaesthesia_end         TIME,
    anaesthesia_notes       TEXT,
    -- Surgery details
    procedure_performed     TEXT,
    findings                TEXT,
    complications           TEXT,
    blood_loss_ml           INTEGER DEFAULT 0,
    transfusion_given       BOOLEAN DEFAULT FALSE,
    transfusion_units       INTEGER DEFAULT 0,
    -- WHO Sign Out
    signout_done            BOOLEAN DEFAULT FALSE,
    instruments_counted     BOOLEAN DEFAULT FALSE,
    swabs_counted           BOOLEAN DEFAULT FALSE,
    specimen_labelled       BOOLEAN DEFAULT FALSE,
    equipment_issues        TEXT,
    -- Outcome
    surgery_outcome         VARCHAR(20) DEFAULT 'SUCCESSFUL',
    surgeon_notes           TEXT,
    recorded_by             VARCHAR(200),
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

-- ── Post-Op Recovery ──────────────────────────────────────────────────────
CREATE TABLE postop_records (
    id                  BIGSERIAL PRIMARY KEY,
    booking_id          BIGINT NOT NULL REFERENCES surgery_bookings(id),
    patient_id          BIGINT NOT NULL REFERENCES patients(id),
    -- Recovery vitals
    arrival_time        TIME,
    bp_systolic         INTEGER,
    bp_diastolic        INTEGER,
    pulse               INTEGER,
    temperature         DECIMAL(4,1),
    spo2                INTEGER,
    pain_score          INTEGER,
    consciousness       VARCHAR(30) DEFAULT 'ALERT',
    -- Recovery progress
    airway_maintained   BOOLEAN DEFAULT TRUE,
    nausea_vomiting     BOOLEAN DEFAULT FALSE,
    bleeding_controlled BOOLEAN DEFAULT TRUE,
    -- Medications in recovery
    analgesia_given     BOOLEAN DEFAULT FALSE,
    analgesia_details   VARCHAR(200),
    antiemetic_given    BOOLEAN DEFAULT FALSE,
    -- Discharge from recovery
    discharge_time      TIME,
    discharged_to       VARCHAR(50),
    aldrete_score       INTEGER,
    complications       TEXT,
    notes               TEXT,
    recorded_by         VARCHAR(200),
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Sequences ─────────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS theatre_rooms_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS surgery_bookings_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS preop_checklists_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS intraop_records_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS postop_records_SEQ
    START WITH 1 INCREMENT BY 50;

-- ── Indexes ───────────────────────────────────────────────────────────────
CREATE INDEX idx_surgery_bookings_patient  ON surgery_bookings(patient_id);
CREATE INDEX idx_surgery_bookings_date     ON surgery_bookings(planned_date);
CREATE INDEX idx_surgery_bookings_status   ON surgery_bookings(status);
CREATE INDEX idx_surgery_bookings_room     ON surgery_bookings(theatre_room_id);
CREATE INDEX idx_preop_booking             ON preop_checklists(booking_id);
CREATE INDEX idx_intraop_booking           ON intraop_records(booking_id);
CREATE INDEX idx_postop_booking            ON postop_records(booking_id);

-- ── Seed Theatre Rooms ────────────────────────────────────────────────────
INSERT INTO theatre_rooms (name, code, room_type, floor) VALUES
    ('Main Theatre',  'THTR-01', 'GENERAL',   'Ground Floor'),
    ('Minor Theatre', 'THTR-02', 'MINOR',      'Ground Floor');