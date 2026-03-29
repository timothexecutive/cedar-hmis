package ke.cedar.hmis.inventory;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "stock_adjustments")
public class StockAdjustment extends PanacheEntity {

    @Column(name = "adjustment_no", unique = true,
            nullable = false)
    public String adjustmentNo;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    public InventoryItem item;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    public InventoryBatch batch;

    @Column(name = "adjustment_type", nullable = false)
    public String adjustmentType;

    @Column(name = "quantity_before", nullable = false)
    public Integer quantityBefore;

    @Column(name = "quantity_change", nullable = false)
    public Integer quantityChange;

    @Column(name = "quantity_after", nullable = false)
    public Integer quantityAfter;

    @Column(nullable = false, columnDefinition = "TEXT")
    public String reason;

    @Column(name = "approved_by")
    public String approvedBy;

    @Column(name = "adjusted_by")
    public String adjustedBy;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static List<StockAdjustment> findByItem(
            Long itemId) {
        return find("item.id = ?1 " +
                    "ORDER BY createdAt DESC",
                itemId).list();
    }

    public static List<StockAdjustment> findByType(
            String type) {
        return find("adjustmentType = ?1 " +
                    "ORDER BY createdAt DESC",
                type).list();
    }

    public static List<StockAdjustment> findRecent() {
        return find("ORDER BY createdAt DESC")
                .page(0, 50).list();
    }
}