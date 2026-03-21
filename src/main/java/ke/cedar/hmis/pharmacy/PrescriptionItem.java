package ke.cedar.hmis.pharmacy;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "prescription_items")
public class PrescriptionItem extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "prescription_id", nullable = false)
    public Prescription prescription;

    @ManyToOne
    @JoinColumn(name = "drug_id", nullable = false)
    public Drug drug;

    public String dosage;
    public String frequency;
    public String duration;

    @Column(nullable = false)
    public Integer quantity;

    public String instructions;

    // PENDING, DISPENSED
    public String status = "PENDING";

    @Column(name = "created_at")
    public LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static List<PrescriptionItem> findByPrescription(Long prescriptionId) {
        return find("prescription.id", prescriptionId).list();
    }
}