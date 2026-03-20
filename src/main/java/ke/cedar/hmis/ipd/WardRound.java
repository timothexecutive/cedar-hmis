package ke.cedar.hmis.ipd;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ward_rounds")
public class WardRound extends PanacheEntity {

    @ManyToOne
    @JoinColumn(name = "admission_id", nullable = false)
    public Admission admission;

    public String doctor;
    public String notes;
    public String plan;

    @Column(name = "round_date")
    public LocalDateTime roundDate;

    @PrePersist
    public void onCreate() {
        roundDate = LocalDateTime.now();
    }

    public static List<WardRound> findByAdmission(Long admissionId) {
        return find("admission.id = ?1 ORDER BY roundDate DESC",
                admissionId).list();
    }
}