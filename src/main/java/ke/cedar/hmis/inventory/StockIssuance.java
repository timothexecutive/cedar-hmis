package ke.cedar.hmis.inventory;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "stock_issuances")
public class StockIssuance extends PanacheEntity {

    @Column(name = "issuance_no", unique = true,
            nullable = false)
    public String issuanceNo;

    @Column(nullable = false)
    public String department;

    @Column(name = "requested_by")
    public String requestedBy;

    @Column(name = "issued_by")
    public String issuedBy;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    public Patient patient;

    @Column(columnDefinition = "TEXT")
    public String purpose;

    public String status = "ISSUED";

    @Column(name = "issued_date")
    public LocalDate issuedDate;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt  = LocalDateTime.now();
        issuedDate = issuedDate != null ?
                issuedDate : LocalDate.now();
    }

    public static List<StockIssuance> findByDepartment(
            String department) {
        return find("department = ?1 " +
                    "ORDER BY issuedDate DESC",
                department).list();
    }

    public static List<StockIssuance> findByPatient(
            Long patientId) {
        return find("patient.id = ?1 " +
                    "ORDER BY issuedDate DESC",
                patientId).list();
    }

    public static List<StockIssuance> findByDate(
            LocalDate date) {
        return find("issuedDate = ?1 " +
                    "ORDER BY createdAt DESC",
                date).list();
    }
}