package ke.cedar.hmis.billing;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import ke.cedar.hmis.opd.Visit;
import ke.cedar.hmis.ipd.Admission;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "invoices")
public class Invoice extends PanacheEntity {

    @Column(name = "invoice_no", unique = true, nullable = false)
    public String invoiceNo;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @ManyToOne
    @JoinColumn(name = "visit_id")
    public Visit visit;

    @ManyToOne
    @JoinColumn(name = "admission_id")
    public Admission admission;

    @Column(name = "invoice_type")
    public String invoiceType = "OPD";

    // PENDING, PARTIAL, PAID, VOID
    public String status = "PENDING";

    public BigDecimal subtotal       = BigDecimal.ZERO;
    public BigDecimal discount       = BigDecimal.ZERO;
    public BigDecimal tax            = BigDecimal.ZERO;

    @Column(name = "total_amount")
    public BigDecimal totalAmount    = BigDecimal.ZERO;

    @Column(name = "insurance_amount")
    public BigDecimal insuranceAmount = BigDecimal.ZERO;

    @Column(name = "patient_amount")
    public BigDecimal patientAmount  = BigDecimal.ZERO;

    @Column(name = "paid_amount")
    public BigDecimal paidAmount     = BigDecimal.ZERO;

    public BigDecimal balance        = BigDecimal.ZERO;

    public String notes;

    @Column(name = "created_by")
    public String createdBy;

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

    public static List<Invoice> findByPatient(Long patientId) {
        return find("patient.id = ?1 ORDER BY createdAt DESC",
                patientId).list();
    }

    public static List<Invoice> findPending() {
        return find("status = 'PENDING' OR status = 'PARTIAL' " +
                    "ORDER BY createdAt ASC").list();
    }

    public static List<Invoice> findByStatus(String status) {
        return find("status = ?1 ORDER BY createdAt DESC",
                status).list();
    }
}