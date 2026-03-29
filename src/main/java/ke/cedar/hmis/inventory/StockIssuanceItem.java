package ke.cedar.hmis.inventory;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "stock_issuance_items")
public class StockIssuanceItem extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "issuance_id", nullable = false)
    public StockIssuance issuance;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    public InventoryItem item;

    @ManyToOne
    @JoinColumn(name = "batch_id")
    public InventoryBatch batch;

    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "unit_cost")
    public BigDecimal unitCost;

    public BigDecimal total;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        if (unitCost != null && quantity != null) {
            total = unitCost.multiply(
                BigDecimal.valueOf(quantity));
        }
    }

    public static List<StockIssuanceItem> findByIssuance(
            Long issuanceId) {
        return find("issuance.id = ?1", issuanceId).list();
    }

    public static List<StockIssuanceItem> findByItem(
            Long itemId) {
        return find("item.id = ?1 " +
                    "ORDER BY createdAt DESC",
                itemId).list();
    }
}