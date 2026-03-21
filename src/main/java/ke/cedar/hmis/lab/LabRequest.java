package ke.cedar.hmis.lab;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.opd.Visit;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "lab_requests")
public class LabRequest extends PanacheEntity {

    @Column(name = "request_no", unique = true, nullable = false)
    public String requestNo;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @ManyToOne
    @JoinColumn(name = "visit_id")
    public Visit visit;

    @Column(name = "requested_by")
    public String requestedBy;

    // PENDING, IN_PROGRESS, COMPLETED
    public String status = "PENDING";

    // ROUTINE, URGENT, STAT
    public String priority = "ROUTINE";

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

    public static List<LabRequest> findPending() {
        return find("status = 'PENDING' OR status = 'IN_PROGRESS' " +
                    "ORDER BY createdAt ASC").list();
    }

    public static List<LabRequest> findByPatient(Long patientId) {
        return find("patient.id = ?1 ORDER BY createdAt DESC",
                patientId).list();
    }
}