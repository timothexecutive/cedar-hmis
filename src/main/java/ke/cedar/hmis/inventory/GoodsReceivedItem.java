package ke.cedar.hmis.inventory;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "goods_received_items")
public class GoodsReceivedItem extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "grn_id", nullable = false)
    public GoodsReceived goodsReceived;

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

    @Column(name = "batch_no")
    public String batchNo;

    @Column(name = "expiry_date")
    public LocalDate expiryDate;

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

    public static List<GoodsReceivedItem> findByGRN(
            Long grnId) {
        return find("goodsReceived.id = ?1", grnId).list();
    }
}