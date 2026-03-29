package ke.cedar.hmis.inventory;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "purchase_order_items")
public class PurchaseOrderItem extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "po_id", nullable = false)
    public PurchaseOrder purchaseOrder;

    @ManyToOne
    @JoinColumn(name = "item_id", nullable = false)
    public InventoryItem item;

    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "unit_cost", nullable = false)
    public BigDecimal unitCost;

    @Column(nullable = false)
    public BigDecimal total;

    @Column(name = "quantity_received")
    public Integer quantityReceived = 0;

    public String status = "PENDING";

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
        total     = unitCost.multiply(
            BigDecimal.valueOf(quantity));
    }

    public static List<PurchaseOrderItem> findByPO(
            Long poId) {
        return find("purchaseOrder.id = ?1", poId).list();
    }
}