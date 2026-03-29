package ke.cedar.hmis.maternity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.ipd.Admission;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "maternity_admissions")
public class MaternityAdmission extends PanacheEntity {

    @Column(name = "admission_no", unique = true, nullable = false)
    public String admissionNo;

    @ManyToOne
    @JoinColumn(name = "pregnancy_id")
    public Pregnancy pregnancy;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @ManyToOne
    @JoinColumn(name = "ipd_admission_id")
    public Admission ipdAdmission;

    @Column(name = "admission_type")
    public String admissionType = "LABOUR";

    @Column(name = "gestation_weeks")
    public Integer gestationWeeks;

    @Column(name = "membranes_status")
    public String membranesStatus = "INTACT";

    @Column(name = "onset_of_labour")
    public String onsetOfLabour = "SPONTANEOUS";

    @Column(name = "labour_started_at")
    public LocalDateTime labourStartedAt;

    @Column(name = "admitted_by")
    public String admittedBy;

    public String status = "IN_LABOUR";

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // ── Queries ───────────────────────────────────────
    public static List<MaternityAdmission> findActive() {
        return find("status = 'IN_LABOUR' " +
                    "ORDER BY createdAt DESC").list();
    }

    public static MaternityAdmission findByPatientActive(
            Long patientId) {
        return find("patient.id = ?1 AND status = 'IN_LABOUR'",
                patientId).firstResult();
    }

    public static List<MaternityAdmission> findByDate(
            java.time.LocalDate date) {
        return find("CAST(createdAt AS DATE) = ?1 " +
                    "ORDER BY createdAt DESC", date).list();
    }
}