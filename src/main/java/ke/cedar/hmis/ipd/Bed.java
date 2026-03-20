package ke.cedar.hmis.ipd;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "beds")
public class Bed extends PanacheEntity {

    @Column(name = "bed_number", nullable = false)
    public String bedNumber;

    @ManyToOne
    @JoinColumn(name = "ward_id", nullable = false)
    public Ward ward;

    public String status = "AVAILABLE";

    @Column(name = "bed_type")
    public String bedType = "GENERAL";

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

    public static List<Bed> findByWard(Long wardId) {
        return find("ward.id = ?1 AND isActive = true ORDER BY bedNumber ASC",
                wardId).list();
    }

    public static List<Bed> findAvailableByWard(Long wardId) {
        return find("ward.id = ?1 AND status = 'AVAILABLE' AND isActive = true",
                wardId).list();
    }

    public static long countAvailable() {
        return find("status", "AVAILABLE").count();
    }

    public static long countOccupied() {
        return find("status", "OCCUPIED").count();
    }
}