package ke.cedar.hmis.opd;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "visits")
public class Visit extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @Column(name = "visit_no", unique = true, nullable = false)
    public String visitNo;

    @Column(name = "visit_date")
    public LocalDate visitDate;

    @Column(name = "visit_type")
    public String visitType = "OPD";

    public String status = "WAITING";

    @Column(name = "queue_number")
    public Integer queueNumber;

    @Column(name = "assigned_doctor")
    public String assignedDoctor;

    @Column(name = "chief_complaint")
    public String chiefComplaint;

    public String diagnosis;
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        visitDate = LocalDate.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static List<Visit> findTodaysQueue() {
        return find("visitDate = ?1 AND visitType = 'OPD' ORDER BY queueNumber ASC",
                LocalDate.now()).list();
    }

    public static List<Visit> findByStatus(String status) {
        return find("visitDate = ?1 AND status = ?2 ORDER BY queueNumber ASC",
                LocalDate.now(), status).list();
    }

    public static long countTodaysQueue() {
        return find("visitDate = ?1 AND visitType = 'OPD'",
                LocalDate.now()).count();
    }
}