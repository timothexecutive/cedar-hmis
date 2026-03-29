package ke.cedar.hmis.inventory;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "inventory_batches")
public class InventoryBatch extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    public InventoryItem item;

    @Column(name = "batch_no", nullable = false)
    public String batchNo;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    public Supplier supplier;

    @Column(name = "manufacture_date")
    public LocalDate manufactureDate;

    @Column(name = "expiry_date")
    public LocalDate expiryDate;

    @Column(name = "quantity_received", nullable = false)
    public Integer quantityReceived;

    @Column(name = "quantity_remaining", nullable = false)
    public Integer quantityRemaining;

    @Column(name = "unit_cost")
    public BigDecimal unitCost;

    @Column(name = "selling_price")
    public BigDecimal sellingPrice;

    @Column(name = "storage_location")
    public String storageLocation;

    @Column(name = "received_date")
    public LocalDate receivedDate;

    @Column(name = "received_by")
    public String receivedBy;

    @Column(name = "is_expired")
    public Boolean isExpired = false;

    @Column(name = "is_active")
    public Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt    = LocalDateTime.now();
        receivedDate = receivedDate != null ?
                receivedDate : LocalDate.now();
        // Auto flag expired on creation
        if (expiryDate != null &&
                expiryDate.isBefore(LocalDate.now())) {
            isExpired = true;
        }
    }

    public boolean isExpiringSoon(int days) {
        if (expiryDate == null) return false;
        return expiryDate.isBefore(
            LocalDate.now().plusDays(days));
    }

    // ── FIFO — oldest batch first ──────────────────────
    public static List<InventoryBatch> findFIFO(
            Long itemId) {
        return find("item.id = ?1 " +
                    "AND isActive = true " +
                    "AND isExpired = false " +
                    "AND quantityRemaining > 0 " +
                    "ORDER BY receivedDate ASC, " +
                    "expiryDate ASC",
                itemId).list();
    }

    public static List<InventoryBatch> findByItem(
            Long itemId) {
        return find("item.id = ?1 " +
                    "ORDER BY receivedDate DESC",
                itemId).list();
    }

    public static List<InventoryBatch> findExpiring(
            int daysAhead) {
        LocalDate threshold =
            LocalDate.now().plusDays(daysAhead);
        return find("expiryDate <= ?1 " +
                    "AND isExpired = false " +
                    "AND isActive = true " +
                    "AND quantityRemaining > 0 " +
                    "ORDER BY expiryDate ASC",
                threshold).list();
    }

    public static List<InventoryBatch> findExpired() {
        return find("isExpired = true " +
                    "AND quantityRemaining > 0 " +
                    "ORDER BY expiryDate ASC").list();
    }

    public static long countAvailableBatches(
            Long itemId) {
        return count("item.id = ?1 " +
                     "AND isActive = true " +
                     "AND isExpired = false " +
                     "AND quantityRemaining > 0",
                itemId);
    }
}