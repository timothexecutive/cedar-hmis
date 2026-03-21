package ke.cedar.hmis.billing;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.reception.Patient;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "insurance_claims")
public class InsuranceClaim extends PanacheEntity {

    @Column(name = "claim_no", unique = true, nullable = false)
    public String claimNo;

    @ManyToOne
    @JoinColumn(name = "invoice_id", nullable = false)
    public Invoice invoice;

    @ManyToOne
    @JoinColumn(name = "provider_id", nullable = false)
    public InsuranceProvider provider;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    public Patient patient;

    @Column(name = "member_no")
    public String memberNo;

    @Column(name = "amount_claimed")
    public BigDecimal amountClaimed  = BigDecimal.ZERO;

    @Column(name = "amount_approved")
    public BigDecimal amountApproved = BigDecimal.ZERO;

    @Column(name = "amount_paid")
    public BigDecimal amountPaid     = BigDecimal.ZERO;

    // SUBMITTED, APPROVED, REJECTED, PAID, QUERIED
    public String status = "SUBMITTED";

    @Column(name = "claim_ref_no")
    public String claimRefNo;

    @Column(name = "submission_date")
    public LocalDateTime submissionDate;

    @Column(name = "approval_date")
    public LocalDateTime approvalDate;

    @Column(name = "payment_date")
    public LocalDateTime paymentDate;

    @Column(name = "rejection_reason")
    public String rejectionReason;

    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt       = LocalDateTime.now();
        updatedAt       = LocalDateTime.now();
        submissionDate  = LocalDateTime.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static List<InsuranceClaim> findByStatus(String status) {
        return find("status = ?1 ORDER BY createdAt DESC",
                status).list();
    }

    public static List<InsuranceClaim> findByProvider(Long providerId) {
        return find("provider.id = ?1 ORDER BY createdAt DESC",
                providerId).list();
    }
}