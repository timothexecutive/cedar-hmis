package ke.cedar.hmis.inventory;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder extends PanacheEntity {

    @Column(name = "lpo_no", unique = true,
            nullable = false)
    public String lpoNo;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    public Supplier supplier;

    @Column(name = "order_date")
    public LocalDate orderDate;

    @Column(name = "expected_date")
    public LocalDate expectedDate;

    @Column(name = "total_amount")
    public BigDecimal totalAmount = BigDecimal.ZERO;

    public String status = "DRAFT";

    @Column(name = "approved_by")
    public String approvedBy;

    @Column(name = "approved_at")
    public LocalDateTime approvedAt;

    @Column(name = "rejection_reason",
            columnDefinition = "TEXT")
    public String rejectionReason;

    @Column(name = "delivery_address",
            columnDefinition = "TEXT")
    public String deliveryAddress;

    @Column(name = "payment_terms")
    public String paymentTerms;

    @Column(name = "raised_by")
    public String raisedBy;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @Column(name = "updated_at")
    public LocalDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        orderDate = orderDate != null ?
                orderDate : LocalDate.now();
    }

    @PreUpdate
    public void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public static List<PurchaseOrder> findByStatus(
            String status) {
        return find("status = ?1 " +
                    "ORDER BY createdAt DESC",
                status).list();
    }

    public static List<PurchaseOrder> findBySupplier(
            Long supplierId) {
        return find("supplier.id = ?1 " +
                    "ORDER BY createdAt DESC",
                supplierId).list();
    }

    public static List<PurchaseOrder> findPendingApproval() {
        return find("status = 'SUBMITTED' " +
                    "ORDER BY createdAt ASC").list();
    }
}