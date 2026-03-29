package ke.cedar.hmis.maternity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "anc_visits")
public class ANCVisit extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "pregnancy_id", nullable = false)
    public Pregnancy pregnancy;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @Column(name = "visit_no")
    public Integer visitNo;

    @Column(name = "visit_date")
    public LocalDate visitDate;

    @Column(name = "gestation_weeks")
    public Integer gestationWeeks;

    public BigDecimal weight;

    @Column(name = "bp_systolic")
    public Integer bpSystolic;

    @Column(name = "bp_diastolic")
    public Integer bpDiastolic;

    public BigDecimal temperature;
    public Integer pulse;

    @Column(name = "fundal_height")
    public BigDecimal fundalHeight;

    @Column(name = "fetal_presentation")
    public String fetalPresentation;

    @Column(name = "fetal_heart_rate")
    public Integer fetalHeartRate;

    public String oedema = "NONE";

    @Column(name = "urine_protein")
    public String urineProtein = "NEGATIVE";

    @Column(name = "urine_glucose")
    public String urineGlucose = "NEGATIVE";

    public BigDecimal haemoglobin;

    @Column(name = "tt_vaccine")
    public Boolean ttVaccine = false;

    @Column(name = "ipt_given")
    public Boolean iptGiven = false;

    @Column(name = "iron_folate")
    public Boolean ironFolate = false;

    @Column(name = "llins_given")
    public Boolean llinsGiven = false;

    @Column(name = "next_visit_date")
    public LocalDate nextVisitDate;

    @Column(name = "risk_flags", columnDefinition = "TEXT")
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

    // ── Auto risk flag detection ───────────────────────
    public void detectRiskFlags() {
        StringBuilder flags = new StringBuilder();

        if (bpSystolic != null && bpDiastolic != null) {
            if (bpSystolic >= 160 || bpDiastolic >= 110)
                flags.append("SEVERE_PREECLAMPSIA;");
            else if (bpSystolic >= 140 || bpDiastolic >= 90)
                flags.append("HYPERTENSION;");
        }

        if (fetalHeartRate != null) {
            if (fetalHeartRate < 100 || fetalHeartRate > 160)
                flags.append("ABNORMAL_FHR;");
        }

        if (haemoglobin != null &&
                haemoglobin.compareTo(new BigDecimal("11.0")) < 0)
            flags.append("ANAEMIA;");

        if ("POSITIVE".equals(urineProtein))
            flags.append("PROTEINURIA;");

        if (!"NONE".equals(oedema))
            flags.append("OEDEMA;");

        this.riskFlags = flags.toString();
    }

    // ── Queries ───────────────────────────────────────
    public static List<ANCVisit> findByPregnancy(Long pregnancyId) {
        return find("pregnancy.id = ?1 ORDER BY visitNo ASC",
                pregnancyId).list();
    }

    public static long countByPregnancy(Long pregnancyId) {
        return count("pregnancy.id = ?1", pregnancyId);
    }

    public static List<ANCVisit> findWithRiskFlags() {
        return find("riskFlags IS NOT NULL AND riskFlags != '' " +
                    "ORDER BY createdAt DESC").list();
    }
}