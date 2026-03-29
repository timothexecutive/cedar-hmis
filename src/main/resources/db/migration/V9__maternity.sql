-- V9: Maternity Module
-- Covers: Pregnancy, ANC Visits, Labour, Delivery, Baby Record, Postnatal

-- ── Pregnancies ───────────────────────────────────────────────────────────
CREATE TABLE pregnancies (
    id                  BIGSERIAL PRIMARY KEY,
    patient_id          BIGINT NOT NULL REFERENCES patients(id),
    pregnancy_no        VARCHAR(50) UNIQUE NOT NULL,
    gravida             INTEGER NOT NULL DEFAULT 1,
    parity              INTEGER NOT NULL DEFAULT 0,
    lmp                 DATE,
    edd                 DATE,
    gestation_at_reg    INTEGER,
    blood_group         VARCHAR(5),
    hiv_status          VARCHAR(20) DEFAULT 'UNKNOWN',
    on_pmtct            BOOLEAN DEFAULT FALSE,
    pmtct_regimen       VARCHAR(100),
    syphilis_status     VARCHAR(20) DEFAULT 'UNKNOWN',
    hepatitis_b         VARCHAR(20) DEFAULT 'UNKNOWN',
    haemoglobin         DECIMAL(5,2),
    has_diabetes        BOOLEAN DEFAULT FALSE,
    has_hypertension    BOOLEAN DEFAULT FALSE,
    previous_cs         BOOLEAN DEFAULT FALSE,
    previous_cs_reason  TEXT,
    risk_level          VARCHAR(20) DEFAULT 'LOW',
    risk_flags          TEXT,
    status              VARCHAR(20) DEFAULT 'ACTIVE',
    registered_by       VARCHAR(200),
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── ANC Visits ────────────────────────────────────────────────────────────
CREATE TABLE anc_visits (
    id                  BIGSERIAL PRIMARY KEY,
    pregnancy_id        BIGINT NOT NULL REFERENCES pregnancies(id),
    patient_id          BIGINT NOT NULL REFERENCES patients(id),
    visit_no            INTEGER NOT NULL DEFAULT 1,
    visit_date          DATE NOT NULL DEFAULT CURRENT_DATE,
    gestation_weeks     INTEGER,
    weight              DECIMAL(5,2),
    bp_systolic         INTEGER,
    bp_diastolic        INTEGER,
    temperature         DECIMAL(4,1),
    pulse               INTEGER,
    fundal_height       DECIMAL(5,2),
    fetal_presentation  VARCHAR(50),
    fetal_heart_rate    INTEGER,
    oedema              VARCHAR(20) DEFAULT 'NONE',
    urine_protein       VARCHAR(20) DEFAULT 'NEGATIVE',
    urine_glucose       VARCHAR(20) DEFAULT 'NEGATIVE',
    haemoglobin         DECIMAL(5,2),
    tt_vaccine          BOOLEAN DEFAULT FALSE,
    ipt_given           BOOLEAN DEFAULT FALSE,
    iron_folate         BOOLEAN DEFAULT FALSE,
    llins_given         BOOLEAN DEFAULT FALSE,
    next_visit_date     DATE,
    risk_flags          TEXT,
    notes               TEXT,
    seen_by             VARCHAR(200),
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Maternity Admissions ──────────────────────────────────────────────────
CREATE TABLE maternity_admissions (
    id                  BIGSERIAL PRIMARY KEY,
    admission_no        VARCHAR(50) UNIQUE NOT NULL,
    pregnancy_id        BIGINT REFERENCES pregnancies(id),
    patient_id          BIGINT NOT NULL REFERENCES patients(id),
    ipd_admission_id    BIGINT REFERENCES admissions(id),
    admission_type      VARCHAR(30) DEFAULT 'LABOUR',
    gestation_weeks     INTEGER,
    membranes_status    VARCHAR(20) DEFAULT 'INTACT',
    onset_of_labour     VARCHAR(20) DEFAULT 'SPONTANEOUS',
    labour_started_at   TIMESTAMPTZ,
    admitted_by         VARCHAR(200),
    status              VARCHAR(20) DEFAULT 'IN_LABOUR',
    notes               TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Deliveries ────────────────────────────────────────────────────────────
CREATE TABLE deliveries (
    id                      BIGSERIAL PRIMARY KEY,
    delivery_no             VARCHAR(50) UNIQUE NOT NULL,
    maternity_admission_id  BIGINT NOT NULL REFERENCES maternity_admissions(id),
    pregnancy_id            BIGINT NOT NULL REFERENCES pregnancies(id),
    patient_id              BIGINT NOT NULL REFERENCES patients(id),
    delivery_date           DATE NOT NULL,
    delivery_time           TIME NOT NULL,
    delivery_type           VARCHAR(30) NOT NULL,
    conducted_by            VARCHAR(200),
    placenta_complete       BOOLEAN DEFAULT TRUE,
    blood_loss_ml           INTEGER DEFAULT 0,
    episiotomy              BOOLEAN DEFAULT FALSE,
    perineal_tear           VARCHAR(20) DEFAULT 'NONE',
    oxytocin_given          BOOLEAN DEFAULT TRUE,
    maternal_condition      VARCHAR(20) DEFAULT 'GOOD',
    maternal_complications  TEXT,
    notes                   TEXT,
    created_at              TIMESTAMPTZ DEFAULT NOW()
);

-- ── Baby Records ──────────────────────────────────────────────────────────
CREATE TABLE baby_records (
    id                  BIGSERIAL PRIMARY KEY,
    delivery_id         BIGINT NOT NULL REFERENCES deliveries(id),
    patient_id          BIGINT REFERENCES patients(id),
    baby_no             VARCHAR(50) UNIQUE NOT NULL,
    gender              VARCHAR(10) NOT NULL,
    birth_weight        DECIMAL(5,3),
    birth_time          TIME NOT NULL,
    gestation_weeks     INTEGER,
    apgar_1min          INTEGER,
    apgar_5min          INTEGER,
    birth_outcome       VARCHAR(20) DEFAULT 'LIVE_BIRTH',
    resuscitation       BOOLEAN DEFAULT FALSE,
    resuscitation_type  VARCHAR(100),
    low_birth_weight    BOOLEAN DEFAULT FALSE,
    breastfed_1hr       BOOLEAN DEFAULT FALSE,
    vitamin_k_given     BOOLEAN DEFAULT FALSE,
    bcg_given           BOOLEAN DEFAULT FALSE,
    polio0_given        BOOLEAN DEFAULT FALSE,
    nevirapine_given    BOOLEAN DEFAULT FALSE,
    birth_notification  VARCHAR(50),
    complications       TEXT,
    notes               TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Postnatal Visits ──────────────────────────────────────────────────────
CREATE TABLE postnatal_visits (
    id                  BIGSERIAL PRIMARY KEY,
    delivery_id         BIGINT NOT NULL REFERENCES deliveries(id),
    patient_id          BIGINT NOT NULL REFERENCES patients(id),
    visit_type          VARCHAR(20) DEFAULT 'MOTHER',
    visit_date          DATE NOT NULL DEFAULT CURRENT_DATE,
    hours_after_birth   INTEGER,
    bp_systolic         INTEGER,
    bp_diastolic        INTEGER,
    temperature         DECIMAL(4,1),
    pulse               INTEGER,
    uterus_involution   VARCHAR(50),
    lochia              VARCHAR(50),
    breastfeeding       VARCHAR(50),
    episiotomy_healing  VARCHAR(50),
    baby_weight         DECIMAL(5,3),
    baby_condition      VARCHAR(50),
    family_planning     VARCHAR(100),
    counselling_done    TEXT,
    next_visit_date     DATE,
    risk_flags          TEXT,
    notes               TEXT,
    seen_by             VARCHAR(200),
    created_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Indexes ───────────────────────────────────────────────────────────────
CREATE INDEX idx_pregnancies_patient     ON pregnancies(patient_id);
CREATE INDEX idx_pregnancies_status      ON pregnancies(status);
CREATE INDEX idx_anc_visits_pregnancy    ON anc_visits(pregnancy_id);
CREATE INDEX idx_anc_visits_patient      ON anc_visits(patient_id);
CREATE INDEX idx_mat_admissions_patient  ON maternity_admissions(patient_id);
CREATE INDEX idx_deliveries_patient      ON deliveries(patient_id);
CREATE INDEX idx_deliveries_date         ON deliveries(delivery_date);
CREATE INDEX idx_baby_records_delivery   ON baby_records(delivery_id);
CREATE INDEX idx_postnatal_delivery      ON postnatal_visits(delivery_id);