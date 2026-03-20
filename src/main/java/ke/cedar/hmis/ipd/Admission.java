package ke.cedar.hmis.ipd;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "admissions")
public class Admission extends PanacheEntity {

    @Column(name = "admission_no", unique = true, nullable = false)
    public String admissionNo;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @ManyToOne
    @JoinColumn(name = "bed_id", nullable = false)
    public Bed bed;

    @ManyToOne
    @JoinColumn(name = "ward_id", nullable = false)
    public Ward ward;

    @Column(name = "admitting_doctor")
    public String admittingDoctor;

    @Column(name = "admission_diagnosis")
    public String admissionDiagnosis;

    @Column(name = "admission_date")
    public LocalDateTime admissionDate;

    @Column(name = "discharge_date")
    public LocalDateTime dischargeDate;

    public String status = "ADMITTED";

    @Column(name = "discharge_summary")
    public String dischargeSummary;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt     = LocalDateTime.now();
        updatedAt     = LocalDateTime.now();
        admissionDate = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static List<Admission> findAllAdmitted() {
        return find("status = 'ADMITTED' ORDER BY admissionDate DESC").list();
    }

    public static List<Admission> findByWard(Long wardId) {
        return find("ward.id = ?1 AND status = 'ADMITTED'", wardId).list();
    }
}