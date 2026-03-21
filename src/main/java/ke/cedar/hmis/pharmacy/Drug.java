package ke.cedar.hmis.pharmacy;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "drugs")
public class Drug extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(name = "generic_name")
    public String genericName;

    @Column(unique = true, nullable = false)
    public String code;

    public String category;
    public String formulation;
    public String strength;
    public String unit;

    @Column(name = "reorder_level")
    public Integer reorderLevel = 10;

    @Column(name = "current_stock")
    public Integer currentStock = 0;

    @Column(name = "buying_price")
    public BigDecimal buyingPrice;

    @Column(name = "selling_price")
    public BigDecimal sellingPrice;

    @Column(name = "is_active")
    public boolean isActive = true;

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

    public static List<Drug> findAllActive() {
        return find("isActive = true ORDER BY name ASC").list();
    }

    public static List<Drug> findLowStock() {
        return find("currentStock <= reorderLevel AND isActive = true").list();
    }

    public static List<Drug> search(String query) {
        String q = "%" + query.toLowerCase() + "%";
        return find("LOWER(name) LIKE ?1 OR LOWER(genericName) LIKE ?2 OR code LIKE ?3",
                q, q, q).list();
    }
}