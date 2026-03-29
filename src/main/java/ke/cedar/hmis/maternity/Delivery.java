package ke.cedar.hmis.maternity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "deliveries")
public class Delivery extends PanacheEntity {

    @Column(name = "delivery_no", unique = true, nullable = false)
    public String deliveryNo;

    @ManyToOne
    @JoinColumn(name = "maternity_admission_id", nullable = false)
    public MaternityAdmission maternityAdmission;

    @ManyToOne
    @JoinColumn(name = "pregnancy_id", nullable = false)
    public Pregnancy pregnancy;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @Column(name = "delivery_date", nullable = false)
    public LocalDate deliveryDate;

    @Column(name = "delivery_time", nullable = false)
    public LocalTime deliveryTime;

    @Column(name = "delivery_type", nullable = false)
    public String deliveryType;

    @Column(name = "conducted_by")
    public String conductedBy;

    @Column(name = "placenta_complete")
    public Boolean placentaComplete = true;

    @Column(name = "blood_loss_ml")
    public Integer bloodLossMl = 0;

    public Boolean episiotomy = false;

    @Column(name = "perineal_tear")
    public String perinealTear = "NONE";

    @Column(name = "oxytocin_given")
    public Boolean oxytocinGiven = true;

    @Column(name = "maternal_condition")
    public String maternalCondition = "GOOD";

    @Column(name = "maternal_complications",
            columnDefinition = "TEXT")
    public String maternalComplications;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt      = LocalDateTime.now();
        deliveryDate   = deliveryDate != null ?
                deliveryDate : LocalDate.now();
        deliveryTime   = deliveryTime != null ?
                deliveryTime : LocalTime.now();
    }

    // ── Queries ───────────────────────────────────────
    public static List<Delivery> findByDate(LocalDate date) {
        return find("deliveryDate = ?1 ORDER BY deliveryTime ASC",
                date).list();
    }

    public static List<Delivery> findByMonth(
            int year, int month) {
        return find("EXTRACT(YEAR FROM deliveryDate) = ?1 " +
                    "AND EXTRACT(MONTH FROM deliveryDate) = ?2 " +
                    "ORDER BY deliveryDate DESC",
                year, month).list();
    }

    public static List<Delivery> findByType(String type) {
        return find("deliveryType = ?1 ORDER BY deliveryDate DESC",
                type).list();
    }

    public static long countByMonth(int year, int month) {
        return count(
                "EXTRACT(YEAR FROM deliveryDate) = ?1 " +
                "AND EXTRACT(MONTH FROM deliveryDate) = ?2",
                year, month);
    }
}