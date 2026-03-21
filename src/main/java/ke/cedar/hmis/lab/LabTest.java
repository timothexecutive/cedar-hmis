package ke.cedar.hmis.lab;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "lab_tests")
public class LabTest extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(unique = true, nullable = false)
    public String code;

    public String category;

    public BigDecimal price;

    public Integer turnaround;

    @Column(name = "is_active")
    public boolean isActive = true;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static List<LabTest> findAllActive() {
        return find("isActive", true).list();
    }

    public static List<LabTest> findByCategory(String category) {
        return find("category = ?1 AND isActive = true",
                category).list();
    }
}