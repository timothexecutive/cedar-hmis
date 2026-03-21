package ke.cedar.hmis.billing;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "insurance_providers")
public class InsuranceProvider extends PanacheEntity {

    @Column(nullable = false)
    public String name;

    @Column(unique = true, nullable = false)
    public String code;

    @Column(name = "provider_type")
    public String providerType = "PRIVATE";

    @Column(name = "contact_person")
    public String contactPerson;

    @Column(name = "contact_phone")
    public String contactPhone;

    @Column(name = "contact_email")
    public String contactEmail;

    @Column(name = "credit_limit")
    public BigDecimal creditLimit;

    @Column(name = "payment_terms")
    public Integer paymentTerms = 30;

    @Column(name = "is_active")
    public boolean isActive = true;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static List<InsuranceProvider> findAllActive() {
        return find("isActive = true ORDER BY name ASC").list();
    }

    public static InsuranceProvider findByCode(String code) {
        return find("code", code).firstResult();
    }
}