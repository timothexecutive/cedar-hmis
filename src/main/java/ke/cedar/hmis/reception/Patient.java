package ke.cedar.hmis.reception;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "patients")
public class Patient extends PanacheEntity {

    @Column(name = "patient_no", unique = true, nullable = false)
    public String patientNo;

    @Column(name = "full_name", nullable = false)
    public String fullName;

    @Column(name = "national_id", unique = true)
    public String nationalId;

    @Column(nullable = false)
    public String phone;

    @Column(nullable = false)
    public String gender;

    @Column(name = "date_of_birth")
    public LocalDate dateOfBirth;

    public String county;

    @Column(name = "next_of_kin_name")
    public String nextOfKinName;

    @Column(name = "next_of_kin_phone")
    public String nextOfKinPhone;

    @Column(name = "sha_member_no")
    public String shaMemberNo;

    @Column(name = "is_sha_member")
    public boolean isSHAMember = false;

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

    public static Patient findByNationalId(String id) {
        return find("nationalId", id).firstResult();
    }

    public static Patient findByPatientNo(String no) {
        return find("patientNo", no).firstResult();
    }

    public static List<Patient> search(String query) {
        String q = "%" + query.toLowerCase() + "%";
        return find("LOWER(fullName) LIKE ?1 OR nationalId LIKE ?2 OR phone LIKE ?3",
                q, q, q).list();
    }

    public static long countActive() {
        return find("isActive", true).count();
    }
}