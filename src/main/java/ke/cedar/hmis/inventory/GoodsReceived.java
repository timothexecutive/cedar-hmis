package ke.cedar.hmis.inventory;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "goods_received")
public class GoodsReceived extends PanacheEntity {

    @Column(name = "grn_no", unique = true,
            nullable = false)
    public String grnNo;

    @ManyToOne
    @JoinColumn(name = "po_id")
    public PurchaseOrder purchaseOrder;

    @ManyToOne
    @JoinColumn(name = "supplier_id", nullable = false)
    public Supplier supplier;

    @Column(name = "received_date")
    public LocalDate receivedDate;

    @Column(name = "invoice_no")
    public String invoiceNo;

    @Column(name = "invoice_date")
    public LocalDate invoiceDate;

    @Column(name = "invoice_amount")
    public BigDecimal invoiceAmount;

    public String status = "RECEIVED";

    @Column(name = "received_by")
    public String receivedBy;

    @Column(name = "verified_by")
    public String verifiedBy;

    @Column(name = "verified_at")
    public LocalDateTime verifiedAt;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt    = LocalDateTime.now();
        receivedDate = receivedDate != null ?
                receivedDate : LocalDate.now();
    }

    public static List<GoodsReceived> findBySupplier(
            Long supplierId) {
        return find("supplier.id = ?1 " +
                    "ORDER BY receivedDate DESC",
                supplierId).list();
    }

    public static List<GoodsReceived> findByStatus(
            String status) {
        return find("status = ?1 " +
                    "ORDER BY createdAt DESC",
                status).list();
    }

    public static List<GoodsReceived> findPendingVerification() {
        return find("status = 'RECEIVED' " +
                    "AND verifiedBy IS NULL " +
                    "ORDER BY createdAt ASC").list();
    }
}