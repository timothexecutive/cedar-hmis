package ke.cedar.hmis.lab;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "lab_results")
public class LabResult extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "request_id", nullable = false)
    public LabRequest request;

    @ManyToOne
    @JoinColumn(name = "test_id", nullable = false)
    public LabTest test;

    @Column(name = "result_value")
    public String resultValue;

    public String unit;

    @Column(name = "reference_range")
    public String referenceRange;

    // NORMAL, HIGH, LOW, CRITICAL
    public String flag = "NORMAL";

    public String notes;

    @Column(name = "done_by")
    public String doneBy;

    @Column(name = "verified_by")
    public String verifiedBy;

    // PENDING, RESULTED, VERIFIED
    public String status = "PENDING";

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

    public static List<LabResult> findByRequest(Long requestId) {
        return find("request.id", requestId).list();
    }
}