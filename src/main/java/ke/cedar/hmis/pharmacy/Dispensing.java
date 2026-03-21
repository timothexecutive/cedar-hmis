package ke.cedar.hmis.pharmacy;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "dispensing")
public class Dispensing extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "prescription_id", nullable = false)
    public Prescription prescription;

    @ManyToOne
    @JoinColumn(name = "drug_id", nullable = false)
    public Drug drug;

    @Column(nullable = false)
    public Integer quantity;

    @Column(name = "dispensed_by")
    public String dispensedBy;

    public String notes;

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static List<Dispensing> findByPrescription(Long prescriptionId) {
        return find("prescription.id", prescriptionId).list();
    }
}