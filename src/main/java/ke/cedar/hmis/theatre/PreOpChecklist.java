package ke.cedar.hmis.theatre;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "preop_checklists")
public class PreOpChecklist extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    public SurgeryBooking booking;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    // ── Patient verification ──────────────────────────
    @Column(name = "identity_confirmed")
    public Boolean identityConfirmed = false;

    @Column(name = "consent_confirmed")
    public Boolean consentConfirmed = false;

    @Column(name = "site_marked")
    public Boolean siteMarked = false;

    // ── Clinical checks ───────────────────────────────
    @Column(name = "anaesthesia_checked")
    public Boolean anaesthesiaChecked = false;

    @Column(name = "pulse_oximeter_working")
    public Boolean pulseOximeterWorking = false;

    @Column(name = "allergies_checked")
    public Boolean allergiesChecked = false;

    @Column(name = "difficult_airway")
    public Boolean difficultAirway = false;

    @Column(name = "aspiration_risk")
    public Boolean aspirationRisk = false;

    @Column(name = "blood_loss_risk")
    public String bloodLossRisk = "LOW";

    // ── Pre-op vitals ─────────────────────────────────
    @Column(name = "bp_systolic")
    public Integer bpSystolic;

    @Column(name = "bp_diastolic")
    public Integer bpDiastolic;

    public Integer pulse;
    public BigDecimal temperature;
    public Integer spo2;
    public BigDecimal weight;

    // ── Preparation ───────────────────────────────────
    @Column(name = "fasting_confirmed")
    public Boolean fastingConfirmed = false;

    @Column(name = "fasting_hours")
    public Integer fastingHours;

    @Column(name = "iv_access")
    public Boolean ivAccess = false;

    @Column(name = "premedication_given")
    public Boolean premedicationGiven = false;

    @Column(name = "premedication_details")
    public String premedicationDetails;

    @Column(name = "jewellery_removed")
    public Boolean jewelleryRemoved = false;

    @Column(name = "nail_polish_removed")
    public Boolean nailPolishRemoved = false;

    // ── Completion ────────────────────────────────────
    @Column(name = "completed_by")
    public String completedBy;

    @Column(name = "completed_at")
    public LocalDateTime completedAt;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt   = LocalDateTime.now();
        completedAt = LocalDateTime.now();
    }

    // ── Safety gate — all critical checks must pass ───
    public boolean isSafeToOperate() {
        return Boolean.TRUE.equals(identityConfirmed)
            && Boolean.TRUE.equals(consentConfirmed)
            && Boolean.TRUE.equals(anaesthesiaChecked)
            && Boolean.TRUE.equals(pulseOximeterWorking)
            && Boolean.TRUE.equals(allergiesChecked)
            && Boolean.TRUE.equals(fastingConfirmed)
            && Boolean.TRUE.equals(ivAccess);
    }

    public static PreOpChecklist findByBooking(
            Long bookingId) {
        return find("booking.id = ?1", bookingId)
                .firstResult();
    }
}