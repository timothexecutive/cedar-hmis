package ke.cedar.hmis.maternity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "pregnancies")
public class Pregnancy extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @Column(name = "pregnancy_no", unique = true, nullable = false)
    public String pregnancyNo;

    public Integer gravida;
    public Integer parity;
    public LocalDate lmp;
    public LocalDate edd;

    @Column(name = "gestation_at_reg")
    public Integer gestationAtReg;

    @Column(name = "blood_group")
    public String bloodGroup;

    @Column(name = "hiv_status")
    public String hivStatus = "UNKNOWN";

    @Column(name = "on_pmtct")
    public Boolean onPmtct = false;

    @Column(name = "pmtct_regimen")
    public String pmtctRegimen;

    @Column(name = "syphilis_status")
    public String syphilisStatus = "UNKNOWN";

    @Column(name = "hepatitis_b")
    public String hepatitisB = "UNKNOWN";

    public BigDecimal haemoglobin;

    @Column(name = "has_diabetes")
    public Boolean hasDiabetes = false;

    @Column(name = "has_hypertension")
    public Boolean hasHypertension = false;

    @Column(name = "previous_cs")
    public Boolean previousCS = false;

    @Column(name = "previous_cs_reason")
    public String previousCSReason;

    @Column(name = "risk_level")
    public String riskLevel = "LOW";

    @Column(name = "risk_flags", columnDefinition = "TEXT")
    public String riskFlags;

    public String status = "ACTIVE";

    @Column(name = "registered_by")
    public String registeredBy;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ── Risk assessment ───────────────────────────────
    public void assessRisk() {
        StringBuilder flags = new StringBuilder();

        if (Boolean.TRUE.equals(previousCS))
            flags.append("PREVIOUS_CS;");
        if ("POSITIVE".equals(hivStatus))
            flags.append("HIV_POSITIVE;");
        if (Boolean.TRUE.equals(hasDiabetes))
            flags.append("DIABETES;");
        if (Boolean.TRUE.equals(hasHypertension))
            flags.append("HYPERTENSION;");
        if (gravida != null && gravida >= 5)
            flags.append("GRAND_MULTIPARA;");
        if (gestationAtReg != null && gestationAtReg > 28)
            flags.append("LATE_ANC_REGISTRATION;");

        this.riskFlags = flags.toString();
        this.riskLevel = flags.length() > 0 ? "HIGH" : "LOW";
    }

    // ── Queries ───────────────────────────────────────
    public static List<Pregnancy> findByPatient(Long patientId) {
        return find("patient.id = ?1 ORDER BY createdAt DESC",
                patientId).list();
    }

    public static List<Pregnancy> findActive() {
        return find("status = 'ACTIVE' ORDER BY createdAt DESC")
                .list();
    }

    public static Pregnancy findActiveByPatient(Long patientId) {
        return find("patient.id = ?1 AND status = 'ACTIVE'",
                patientId).firstResult();
    }

    public static List<Pregnancy> findHighRisk() {
        return find("riskLevel = 'HIGH' AND status = 'ACTIVE' " +
                    "ORDER BY createdAt DESC").list();
    }
}