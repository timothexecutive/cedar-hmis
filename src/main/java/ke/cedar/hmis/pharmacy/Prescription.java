package ke.cedar.hmis.pharmacy;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.opd.Visit;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "prescriptions")
public class Prescription extends PanacheEntity {

    @Column(name = "prescription_no", unique = true, nullable = false)
    public String prescriptionNo;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @ManyToOne
    @JoinColumn(name = "visit_id")
    public Visit visit;

    @Column(name = "prescribed_by")
    public String prescribedBy;

    // PENDING, DISPENSED, PARTIAL
    public String status = "PENDING";

    public String notes;

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

    public static List<Prescription> findPending() {
        return find("status = 'PENDING' OR status = 'PARTIAL' " +
                    "ORDER BY createdAt ASC").list();
    }

    public static List<Prescription> findByPatient(Long patientId) {
        return find("patient.id = ?1 ORDER BY createdAt DESC",
                patientId).list();
    }
}