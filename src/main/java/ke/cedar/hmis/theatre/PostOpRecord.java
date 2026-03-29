package ke.cedar.hmis.theatre;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "postop_records")
public class PostOpRecord extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "booking_id", nullable = false)
    public SurgeryBooking booking;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    // ── Recovery vitals ───────────────────────────────
    @Column(name = "arrival_time")
    public LocalTime arrivalTime;

    @Column(name = "bp_systolic")
    public Integer bpSystolic;

    @Column(name = "bp_diastolic")
    public Integer bpDiastolic;

    public Integer pulse;
    public BigDecimal temperature;
    public Integer spo2;

    @Column(name = "pain_score")
    public Integer painScore;

    public String consciousness = "ALERT";

    // ── Recovery progress ─────────────────────────────
    @Column(name = "airway_maintained")
    public Boolean airwayMaintained = true;

    @Column(name = "nausea_vomiting")
    public Boolean nauseaVomiting = false;

    @Column(name = "bleeding_controlled")
    public Boolean bleedingControlled = true;

    // ── Medications ───────────────────────────────────
    @Column(name = "analgesia_given")
    public Boolean analgesiaGiven = false;

    @Column(name = "analgesia_details")
    public String analgesiaDetails;

    @Column(name = "antiemetic_given")
    public Boolean antiemeticGiven = false;

    // ── Discharge from recovery ───────────────────────
    @Column(name = "discharge_time")
    public LocalTime dischargeTime;

    @Column(name = "discharged_to")
    public String dischargedTo;

    @Column(name = "aldrete_score")
    public Integer aldreteScore;

    @Column(columnDefinition = "TEXT")
    public String complications;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "recorded_by")
    public String recordedBy;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt   = LocalDateTime.now();
        arrivalTime = arrivalTime != null ?
                arrivalTime : LocalTime.now();
    }

    // ── Aldrete score >= 9 means safe to discharge ────
    public boolean isSafeToDischarge() {
        return aldreteScore != null && aldreteScore >= 9;
    }

    public static List<PostOpRecord> findByBooking(
            Long bookingId) {
        return find("booking.id = ?1 " +
                    "ORDER BY createdAt DESC",
                bookingId).list();
    }
}