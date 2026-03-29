package ke.cedar.hmis.inventory;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "suppliers")
public class Supplier extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(unique = true, nullable = false)
    public String code;

    @Column(name = "contact_person")
    public String contactPerson;

    public String phone;
    public String email;

    @Column(columnDefinition = "TEXT")
    public String address;

    @Column(name = "kra_pin")
    public String kraPin;

    @Column(name = "supplier_type")
    public String supplierType = "PRIVATE";

    @Column(name = "credit_days")
    public Integer creditDays = 30;

    @Column(name = "credit_limit")
    public BigDecimal creditLimit = BigDecimal.ZERO;

    @Column(name = "is_active")
    public Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static List<Supplier> findAllActive() {
        return find("isActive = true ORDER BY name ASC")
                .list();
    }

    public static Supplier findByCode(String code) {
        return find("code", code).firstResult();
    }

    public static List<Supplier> findByType(String type) {
        return find("supplierType = ?1 AND isActive = true",
                type).list();
    }
}