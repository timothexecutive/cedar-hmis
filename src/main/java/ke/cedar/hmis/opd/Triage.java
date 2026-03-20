package ke.cedar.hmis.opd;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "triage")
public class Triage extends PanacheEntity {

    @OneToOne
    @JoinColumn(name = "visit_id", nullable = false)
    public Visit visit;

    @Column(name = "blood_pressure")
    public String bloodPressure;

    public BigDecimal temperature;
    public Integer pulse;
    public BigDecimal weight;
    public BigDecimal height;
    public Integer spo2;
    public BigDecimal rbs;

    @Column(name = "triage_category")
    public String triageCategory = "NON_URGENT";

    public String notes;

    @Column(name = "done_by")
    public String doneBy;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public void calculateTriageCategory() {
        boolean urgent   = false;
        boolean moderate = false;

        if (spo2 != null && spo2 < 90) urgent = true;
        if (spo2 != null && spo2 < 94) moderate = true;
        if (temperature != null &&
            temperature.compareTo(new BigDecimal("39.5")) > 0) urgent = true;
        if (temperature != null &&
            temperature.compareTo(new BigDecimal("38.5")) > 0) moderate = true;
        if (pulse != null && (pulse > 120 || pulse < 50)) urgent = true;
        if (rbs != null && (rbs.compareTo(new BigDecimal("3.0")) < 0 ||
            rbs.compareTo(new BigDecimal("20.0")) > 0)) urgent = true;

        if (urgent)         triageCategory = "URGENT";
        else if (moderate)  triageCategory = "MODERATE";
        else                triageCategory = "NON_URGENT";
    }
}