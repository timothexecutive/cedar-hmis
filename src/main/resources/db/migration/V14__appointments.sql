-- V14: Appointment Booking Module
-- Covers: Doctor Schedules, Appointments, SMS OTP (Phase 2)

-- ── Doctor Schedules ──────────────────────────────────────────────────────
CREATE TABLE doctor_schedules (
    id                  BIGSERIAL PRIMARY KEY,
    doctor_name         VARCHAR(200) NOT NULL,
    doctor_email        VARCHAR(200),
    department          VARCHAR(100),
    -- Comma-separated: MON,TUE,WED,THU,FRI,SAT,SUN
    working_days        VARCHAR(100) NOT NULL DEFAULT 'MON,TUE,WED,THU,FRI',
    start_time          TIME NOT NULL DEFAULT '08:00',
    end_time            TIME NOT NULL DEFAULT '17:00',
    slot_duration_mins  INTEGER NOT NULL DEFAULT 30,
    max_slots_per_day   INTEGER NOT NULL DEFAULT 16,
    consultation_fee    DECIMAL(10,2) DEFAULT 500.00,
    is_active           BOOLEAN DEFAULT TRUE,
    notes               TEXT,
    created_at          TIMESTAMPTZ DEFAULT NOW(),
    updated_at          TIMESTAMPTZ DEFAULT NOW()
);

-- ── Appointments ──────────────────────────────────────────────────────────
CREATE TABLE appointments (
    id                      BIGSERIAL PRIMARY KEY,
    appointment_no          VARCHAR(50) UNIQUE NOT NULL,
    schedule_id             BIGINT NOT NULL REFERENCES doctor_schedules(id),
    patient_id              BIGINT REFERENCES patients(id),
    -- Patient details captured at booking (even if not yet registered)
    patient_name            VARCHAR(200) NOT NULL,
    patient_phone           VARCHAR(20) NOT NULL,
    patient_national_id     VARCHAR(50),
    doctor_name             VARCHAR(200) NOT NULL,
    department              VARCHAR(100),
    appointment_date        DATE NOT NULL,
    appointment_time        TIME NOT NULL,
    reason                  TEXT,
    -- SCHEDULED, CONFIRMED, ARRIVED, CANCELLED, NO_SHOW, COMPLETED
    status                  VARCHAR(30) NOT NULL DEFAULT 'SCHEDULED',
    -- Payment
    payment_type            VARCHAR(20) NOT NULL DEFAULT 'CASH', -- CASH, INSURANCE, SHA
    consultation_fee        DECIMAL(10,2),
    -- M-Pesa (cash patients)
    mpesa_checkout_id       VARCHAR(100),
    mpesa_receipt           VARCHAR(100),
    payment_verified        BOOLEAN DEFAULT FALSE,
    -- Insurance patients
    insurance_provider      VARCHAR(100),
    insurance_member_no     VARCHAR(100),
    insurance_verified      BOOLEAN DEFAULT FALSE, -- set by receptionist on arrival
    -- Cancellation
    cancellation_reason     TEXT,
    cancelled_at            TIMESTAMPTZ,
    cancellation_deadline   TIMESTAMPTZ, -- 2hrs before appointment
    refund_issued           BOOLEAN DEFAULT FALSE,
    refund_ref              VARCHAR(100),
    -- Arrival + Visit creation
    arrived_at              TIMESTAMPTZ,
    marked_arrived_by       VARCHAR(200),
    visit_id                BIGINT REFERENCES visits(id),
    -- Booking source
    booking_source          VARCHAR(20) DEFAULT 'RECEPTION', -- RECEPTION, ONLINE, PHONE
    booked_by               VARCHAR(200),
    notes                   TEXT,
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW(),
    -- THE KEY CONSTRAINT: no double booking per doctor per slot
    UNIQUE(schedule_id, appointment_date, appointment_time)
);

-- ── Sequences ─────────────────────────────────────────────────────────────
CREATE SEQUENCE IF NOT EXISTS doctor_schedules_SEQ
    START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE IF NOT EXISTS appointments_SEQ
    START WITH 1 INCREMENT BY 50;

-- ── Indexes ───────────────────────────────────────────────────────────────
CREATE INDEX idx_appointments_date
    ON appointments(appointment_date);
CREATE INDEX idx_appointments_patient
    ON appointments(patient_id);
CREATE INDEX idx_appointments_schedule
    ON appointments(schedule_id);
CREATE INDEX idx_appointments_status
    ON appointments(status);
CREATE INDEX idx_appointments_phone
    ON appointments(patient_phone);

-- ── Seed Doctor Schedules ─────────────────────────────────────────────────
-- Replace with Cedar's actual doctors
INSERT INTO doctor_schedules
    (doctor_name, department, working_days,
     start_time, end_time, slot_duration_mins,
     max_slots_per_day, consultation_fee)
VALUES
    ('Dr. Ooko',    'General Medicine',
     'MON,TUE,WED,THU,FRI', '08:00', '17:00', 30, 16, 500.00),
    ('Dr. Omondi',  'Surgery',
     'MON,TUE,WED,THU,FRI', '08:00', '13:00', 30, 10, 800.00),
    ('Dr. Wanjiru', 'Maternity',
     'MON,TUE,WED,THU,FRI,SAT', '08:00', '17:00', 30, 16, 600.00);