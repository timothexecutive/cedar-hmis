package ke.cedar.hmis.maternity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "baby_records")
public class BabyRecord extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "delivery_id", nullable = false)
    public Delivery delivery;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    public Patient patient;

    @Column(name = "baby_no", unique = true, nullable = false)
    public String babyNo;

    @Column(nullable = false)
    public String gender;

    @Column(name = "birth_weight")
    public BigDecimal birthWeight;

    @Column(name = "birth_time", nullable = false)
    public LocalTime birthTime;

    @Column(name = "gestation_weeks")
    public Integer gestationWeeks;

    @Column(name = "apgar_1min")
    public Integer apgar1Min;

    @Column(name = "apgar_5min")
    public Integer apgar5Min;

    @Column(name = "birth_outcome")
    public String birthOutcome = "LIVE_BIRTH";

    public Boolean resuscitation = false;

    @Column(name = "resuscitation_type")
    public String resuscitationType;

    @Column(name = "low_birth_weight")
    public Boolean lowBirthWeight = false;

    @Column(name = "breastfed_1hr")
    public Boolean breastfed1Hr = false;

    @Column(name = "vitamin_k_given")
    public Boolean vitaminKGiven = false;

    @Column(name = "bcg_given")
    public Boolean bcgGiven = false;

    @Column(name = "polio0_given")
    public Boolean polio0Given = false;

    @Column(name = "nevirapine_given")
    public Boolean nevirapineGiven = false;

    @Column(name = "birth_notification")
    public String birthNotification;

    @Column(columnDefinition = "TEXT")
    public String complications;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        birthTime = birthTime != null ?
                birthTime : LocalTime.now();

        // Auto flag low birth weight
        if (birthWeight != null &&
                birthWeight.compareTo(new BigDecimal("2.5")) < 0) {
            lowBirthWeight = true;
        }
    }

    // ── Queries ───────────────────────────────────────
    public static List<BabyRecord> findByDelivery(
            Long deliveryId) {
        return find("delivery.id = ?1", deliveryId).list();
    }

    public static List<BabyRecord> findLowBirthWeight() {
        return find("lowBirthWeight = true " +
                    "ORDER BY createdAt DESC").list();
    }

    public static List<BabyRecord> findByOutcome(
            String outcome) {
        return find("birthOutcome = ?1 " +
                    "ORDER BY createdAt DESC", outcome).list();
    }
}