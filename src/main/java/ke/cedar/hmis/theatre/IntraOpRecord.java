package ke.cedar.hmis.theatre;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "intraop_records")
public class IntraOpRecord extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    public SurgeryBooking booking;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    // ── WHO Time Out ──────────────────────────────────
    @Column(name = "timeout_done")
    public Boolean timeoutDone = false;

    @Column(name = "team_introduced")
    public Boolean teamIntroduced = false;

    @Column(name = "site_confirmed")
    public Boolean siteConfirmed = false;

    @Column(name = "antibiotic_given")
    public Boolean antibioticGiven = false;

    @Column(name = "antibiotic_name")
    public String antibioticName;

    // ── Surgery timing ────────────────────────────────
    @Column(name = "actual_start_time")
    public LocalTime actualStartTime;

    @Column(name = "actual_end_time")
    public LocalTime actualEndTime;

    @Column(name = "duration_minutes")
    public Integer durationMinutes;

    // ── Anaesthesia ───────────────────────────────────
    @Column(name = "anaesthesia_type")
    public String anaesthesiaType;

    @Column(name = "anaesthesia_start")
    public LocalTime anaesthesiaStart;

    @Column(name = "anaesthesia_end")
    public LocalTime anaesthesiaEnd;

    @Column(name = "anaesthesia_notes",
            columnDefinition = "TEXT")
    public String anaesthesiaNotes;

    // ── Surgery details ───────────────────────────────
    @Column(name = "procedure_performed",
            columnDefinition = "TEXT")
    public String procedurePerformed;

    @Column(columnDefinition = "TEXT")
    public String findings;

    @Column(columnDefinition = "TEXT")
    public String complications;

    @Column(name = "blood_loss_ml")
    public Integer bloodLossMl = 0;

    @Column(name = "transfusion_given")
    public Boolean transfusionGiven = false;

    @Column(name = "transfusion_units")
    public Integer transfusionUnits = 0;

    // ── WHO Sign Out ──────────────────────────────────
    @Column(name = "signout_done")
    public Boolean signoutDone = false;

    @Column(name = "instruments_counted")
    public Boolean instrumentsCounted = false;

    @Column(name = "swabs_counted")
    public Boolean swabsCounted = false;

    @Column(name = "specimen_labelled")
    public Boolean specimenLabelled = false;

    @Column(name = "equipment_issues",
            columnDefinition = "TEXT")
    public String equipmentIssues;

    // ── Outcome ───────────────────────────────────────
    @Column(name = "surgery_outcome")
    public String surgeryOutcome = "SUCCESSFUL";

    @Column(name = "surgeon_notes",
            columnDefinition = "TEXT")
    public String surgeonNotes;

    @Column(name = "recorded_by")
    public String recordedBy;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (actualStartTime != null &&
                actualEndTime != null) {
            durationMinutes = (int)
                java.time.Duration.between(
                    actualStartTime, actualEndTime)
                .toMinutes();
        }
    }

    // ── WHO Sign Out safety gate ───────────────────────
    public boolean isSignOutComplete() {
        return Boolean.TRUE.equals(instrumentsCounted)
            && Boolean.TRUE.equals(swabsCounted);
    }

    public static IntraOpRecord findByBooking(
            Long bookingId) {
        return find("booking.id = ?1", bookingId)
                .firstResult();
    }
}