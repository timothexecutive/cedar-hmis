package ke.cedar.hmis.inventory;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import ke.cedar.hmis.pharmacy.Drug;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "inventory_items")
public class InventoryItem extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(unique = true, nullable = false)
    public String code;

    @Column(nullable = false)
    public String category;

    @Column(name = "sub_category")
    public String subCategory;

    @Column(name = "unit_of_measure", nullable = false)
    public String unitOfMeasure;

    @Column(columnDefinition = "TEXT")
    public String description;

    @Column(name = "reorder_level")
    public Integer reorderLevel = 10;

    @Column(name = "reorder_quantity")
    public Integer reorderQuantity = 50;

    @Column(name = "storage_condition")
    public String storageCondition = "ROOM_TEMP";

    @Column(name = "is_controlled")
    public Boolean isControlled = false;

    @Column(name = "requires_cold_chain")
    public Boolean requiresColdChain = false;

    @Column(name = "is_drug")
    public Boolean isDrug = false;

    @ManyToOne
    @JoinColumn(name = "drug_id")
    public Drug drug;

    @Column(name = "is_active")
    public Boolean isActive = true;

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

    // ── Current total stock across all batches ────────
    public int getCurrentStock() {
        Long total = InventoryBatch.find(
            "item.id = ?1 AND isActive = true " +
            "AND isExpired = false", this.id)
            .stream()
            .mapToLong(b ->
                ((InventoryBatch) b).quantityRemaining)
            .sum();
        return total.intValue();
    }

    public boolean isBelowReorderLevel() {
        return getCurrentStock() <= reorderLevel;
    }

    // ── Queries ───────────────────────────────────────
    public static List<InventoryItem> findAllActive() {
        return find("isActive = true ORDER BY category, " +
                    "name ASC").list();
    }

    public static List<InventoryItem> findByCategory(
            String category) {
        return find("category = ?1 AND isActive = true " +
                    "ORDER BY name ASC", category).list();
    }

    public static List<InventoryItem> search(
            String query) {
        return find("LOWER(name) LIKE LOWER(?1) " +
                    "OR LOWER(code) LIKE LOWER(?1) " +
                    "AND isActive = true",
                "%" + query + "%").list();
    }

    public static List<InventoryItem> findControlled() {
        return find("isControlled = true " +
                    "AND isActive = true " +
                    "ORDER BY name ASC").list();
    }
}