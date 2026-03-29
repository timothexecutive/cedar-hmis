package ke.cedar.hmis.maternity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "postnatal_visits")
public class PostnatalVisit extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "delivery_id", nullable = false)
    public Delivery delivery;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @Column(name = "visit_type")
    public String visitType = "MOTHER";

    @Column(name = "visit_date")
    public LocalDate visitDate;

    @Column(name = "hours_after_birth")
    public Integer hoursAfterBirth;

    @Column(name = "bp_systolic")
    public Integer bpSystolic;

    @Column(name = "bp_diastolic")
    public Integer bpDiastolic;

    public BigDecimal temperature;
    public Integer pulse;

    @Column(name = "uterus_involution")
    public String uterusInvolution;

    public String lochia;
    public String breastfeeding;

    @Column(name = "episiotomy_healing")
    public String episiotomyHealing;

    @Column(name = "baby_weight")
    public BigDecimal babyWeight;

    @Column(name = "baby_condition")
    public String babyCondition;

    @Column(name = "family_planning")
    public String familyPlanning;

    @Column(name = "counselling_done",
            columnDefinition = "TEXT")
    public String counsellingDone;

    @Column(name = "next_visit_date")
    public LocalDate nextVisitDate;

    @Column(name = "risk_flags",
            columnDefinition = "TEXT")
    public String riskFlags;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "seen_by")
    public String seenBy;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        visitDate = visitDate != null ?
                visitDate : LocalDate.now();
    }

    // ── Auto risk detection ───────────────────────────
    public void detectRiskFlags() {
        StringBuilder flags = new StringBuilder();

        if (bpSystolic != null && bpDiastolic != null) {
            if (bpSystolic >= 160 || bpDiastolic >= 110)
                flags.append("SEVERE_HYPERTENSION;");
            else if (bpSystolic >= 140 || bpDiastolic >= 90)
                flags.append("HYPERTENSION;");
        }

        if (temperature != null &&
                temperature.compareTo(new BigDecimal("38.0")) > 0)
            flags.append("FEVER;");

        this.riskFlags = flags.toString();
    }

    // ── Queries ───────────────────────────────────────
    public static List<PostnatalVisit> findByDelivery(
            Long deliveryId) {
        return find("delivery.id = ?1 ORDER BY visitDate ASC",
                deliveryId).list();
    }

    public static List<PostnatalVisit> findWithRiskFlags() {
        return find("riskFlags IS NOT NULL AND riskFlags != '' " +
                    "ORDER BY createdAt DESC").list();
    }
}